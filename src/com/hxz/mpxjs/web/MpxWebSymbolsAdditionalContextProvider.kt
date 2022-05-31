// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.web

import com.intellij.javascript.web.symbols.*
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_ELEMENTS
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_EVENTS
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_SLOTS
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_VUE_COMPONENTS
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_VUE_COMPONENT_PROPS
import com.intellij.javascript.web.symbols.WebSymbol.Companion.KIND_HTML_VUE_DIRECTIVES
import com.intellij.javascript.web.symbols.WebSymbol.Companion.VUE_FRAMEWORK
import com.intellij.javascript.web.symbols.WebSymbol.Priority
import com.intellij.javascript.web.symbols.WebSymbolsContainer.Companion.NAMESPACE_HTML
import com.intellij.javascript.web.symbols.WebSymbolsContainer.Namespace
import com.intellij.lang.javascript.DialectDetector
import com.intellij.lang.javascript.library.JSLibraryUtil
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.settings.JSApplicationSettings
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlTag
import com.intellij.util.castSafelyTo
import com.intellij.util.containers.Stack
import com.hxz.mpxjs.codeInsight.detectMpxScriptLanguage
import com.hxz.mpxjs.codeInsight.documentation.MpxDocumentedItem
import com.hxz.mpxjs.codeInsight.fromAsset
import com.hxz.mpxjs.codeInsight.tags.MpxInsertHandler
import com.hxz.mpxjs.codeInsight.toAsset
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.model.*
import com.hxz.mpxjs.model.MpxModelDirectiveProperties.Companion.DEFAULT_EVENT
import com.hxz.mpxjs.model.MpxModelDirectiveProperties.Companion.DEFAULT_PROP
import com.hxz.mpxjs.model.source.MpxUnresolvedComponent
import java.util.*

class MpxWebSymbolsAdditionalContextProvider : WebSymbolsAdditionalContextProvider {

  companion object {
    const val KIND_MPX_TOP_LEVEL_ELEMENTS = "mpx-file-top-elements"
    const val KIND_MPX_AVAILABLE_SLOTS = "mpx-available-slots"
    const val KIND_MPX_MODEL = "mpx-model"
    const val KIND_MPX_DIRECTIVE_ARGUMENT = "argument"
    const val KIND_MPX_DIRECTIVE_MODIFIERS = "modifiers"

    const val PROP_MPX_MODEL_PROP = "prop"
    const val PROP_MPX_MODEL_EVENT = "event"

    private fun <T> List<T>.mapWithNameFilter(name: String?, mapper: (T) -> WebSymbol): List<WebSymbol> =
      if (name != null) {
        asSequence()
          .map(mapper)
          .filter { it.name == name }
          .toList()
      }
      else this.map(mapper)

  }

  override fun getAdditionalContext(element: PsiElement?, framework: String?): List<WebSymbolsContainer> =
    element
      ?.takeIf { framework == VUE_FRAMEWORK }
      ?.let { MpxModelManager.findEnclosingContainer(it) }
      ?.let {
        listOfNotNull(EntityContainerWrapper(element.containingFile.originalFile, it,
                                             (element as? XmlTag)?.let { tag -> tag.parentTag == null } == true),
                      (element as? XmlTag)?.let { tag -> AvailableSlotsContainer(tag) })
      }
    ?: emptyList()

  class MpxSymbolsCodeCompletionItemCustomizer : WebSymbolCodeCompletionItemCustomizer {
    override fun customize(codeCompletionItem: WebSymbolCodeCompletionItem,
                           namespace: Namespace, kind: SymbolKind): WebSymbolCodeCompletionItem =
      if (namespace == Namespace.HTML)
        when (kind) {
          WebSymbol.KIND_HTML_ATTRIBUTES ->
            codeCompletionItem.source
              ?.takeIf { it.kind == KIND_HTML_VUE_COMPONENT_PROPS }
              ?.jsType?.getTypeText(JSType.TypeTextFormat.PRESENTABLE)
              ?.let { codeCompletionItem.withTypeText(it) }
            ?: codeCompletionItem
          else -> codeCompletionItem
        }
      else codeCompletionItem
  }

  private abstract class MpxWrapperBase : WebSymbolsContainer {

    val namespace: Namespace
      get() = Namespace.HTML

  }

  private class EntityContainerWrapper(private val containingFile: PsiFile,
                                       private val container: MpxEntitiesContainer,
                                       private val isTopLevelTag: Boolean) : MpxWrapperBase() {

    override fun hashCode(): Int = containingFile.hashCode()

    override fun equals(other: Any?): Boolean =
      other is EntityContainerWrapper
      && other.containingFile == containingFile
      && other.container == container
      && other.isTopLevelTag == isTopLevelTag

    override fun getModificationCount(): Long =
      PsiModificationTracker.SERVICE.getInstance(containingFile.project).modificationCount +
      VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.modificationCount

    override fun getSymbols(namespace: Namespace?,
                            kind: String,
                            name: String?,
                            params: WebSymbolsNameMatchQueryParams,
                            context: Stack<WebSymbolsContainer>): List<WebSymbolsContainer> =
      if (namespace == null || namespace == Namespace.HTML)
        when (kind) {
          KIND_HTML_ELEMENTS -> {
            if (containingFile.virtualFile?.fileType == MpxFileType.INSTANCE && isTopLevelTag) {
              params.registry.runNameMatchQuery(
                if (name == null) listOf(NAMESPACE_HTML, KIND_MPX_TOP_LEVEL_ELEMENTS)
                else listOf(NAMESPACE_HTML, KIND_MPX_TOP_LEVEL_ELEMENTS, name),
                context = context
              )
                .map {
                  WebSymbolMatch(it.name, it.nameSegments, Namespace.HTML, KIND_HTML_ELEMENTS, it.context)
                }
            }
            else emptyList()
          }
          KIND_HTML_VUE_COMPONENTS -> {
            val result = mutableListOf<MpxComponent>()
            val normalizedTagName = name?.let { fromAsset(it) }
            container.acceptEntities(object : MpxModelProximityVisitor() {
              override fun visitComponent(name: String, component: MpxComponent, proximity: Proximity): Boolean {
                return acceptSameProximity(proximity, normalizedTagName == null || fromAsset(name) == normalizedTagName) {
                  // Cannot self refer without export declaration with component name
                  if ((component.source as? JSImplicitElement)?.context != containingFile) {
                    result.add(component)
                  }
                }
              }
            }, MpxModelVisitor.Proximity.GLOBAL)
            result.mapNotNull {
              ComponentWrapper(name ?: it.defaultName ?: return@mapNotNull null, it)
            }
          }
          KIND_HTML_VUE_DIRECTIVES -> {
            val searchName = name?.let { fromAsset(it) }
            val directives = mutableListOf<MpxDirective>()
            container.acceptEntities(object : MpxModelProximityVisitor() {
              override fun visitDirective(name: String, directive: MpxDirective, proximity: Proximity): Boolean {
                return acceptSameProximity(proximity, searchName == null || fromAsset(name) == searchName) {
                  directives.add(directive)
                }
              }
            }, MpxModelVisitor.Proximity.GLOBAL)
            directives.mapNotNull {
              DirectiveWrapper(name ?: it.defaultName ?: return@mapNotNull null, it)
            }
          }
          else -> emptyList()
        }
      else emptyList()

    override fun getCodeCompletions(namespace: Namespace?,
                                    kind: String,
                                    name: String?,
                                    params: WebSymbolsCodeCompletionQueryParams,
                                    context: Stack<WebSymbolsContainer>): List<WebSymbolCodeCompletionItem> =
      if (namespace == null || namespace == Namespace.HTML)
        when (kind) {
          KIND_HTML_ELEMENTS -> {
            if (containingFile.virtualFile?.fileType == MpxFileType.INSTANCE && isTopLevelTag) {
              params.registry.runCodeCompletionQuery(
                if (name == null) listOf(NAMESPACE_HTML, KIND_MPX_TOP_LEVEL_ELEMENTS)
                else listOf(NAMESPACE_HTML, KIND_MPX_TOP_LEVEL_ELEMENTS, name),
                params.position, context = context
              )
            }
            else emptyList()
          }
          KIND_HTML_VUE_COMPONENTS -> {
            val result = mutableListOf<WebSymbolCodeCompletionItem>()
            val scriptLanguage = detectMpxScriptLanguage(containingFile)
            container.acceptEntities(object : MpxModelVisitor() {
              override fun visitComponent(name: String, component: MpxComponent, proximity: Proximity): Boolean {
                // Cannot self refer without export declaration with component name
                if ((component.source as? JSImplicitElement)?.context == containingFile) {
                  return true
                }
                // TODO replace with params.registry.getNameVariants(MPX_FRAMEWORK, Namespace.HTML, kind, name)
                listOf(toAsset(name).capitalize(), fromAsset(name)).forEach {
                  result.add(createMpxComponentLookup(component, it, scriptLanguage, proximity))
                }
                return true
              }
            }, MpxModelVisitor.Proximity.OUT_OF_SCOPE)
            result
          }
          KIND_HTML_VUE_DIRECTIVES -> {
            val result = mutableListOf<WebSymbolCodeCompletionItem>()
            container.acceptEntities(object : MpxModelVisitor() {
              override fun visitDirective(name: String, directive: MpxDirective, proximity: Proximity): Boolean {
                result.add(WebSymbolCodeCompletionItem.create(fromAsset(name),
                                                              source = DirectiveWrapper(name, directive),
                                                              priority = priorityOf(proximity)))
                return true
              }
            }, MpxModelVisitor.Proximity.GLOBAL)
            result
          }
          else -> emptyList()
        }
      else emptyList()

    private fun priorityOf(proximity: MpxModelVisitor.Proximity): Priority =
      when (proximity) {
        MpxModelVisitor.Proximity.LOCAL -> Priority.HIGHEST
        MpxModelVisitor.Proximity.APP -> Priority.HIGH
        MpxModelVisitor.Proximity.PLUGIN, MpxModelVisitor.Proximity.GLOBAL -> Priority.NORMAL
        MpxModelVisitor.Proximity.OUT_OF_SCOPE -> Priority.LOW
      }

    private fun createMpxComponentLookup(component: MpxComponent,
                                         name: String,
                                         scriptLanguage: String?,
                                         proximity: MpxModelVisitor.Proximity): WebSymbolCodeCompletionItem {
      val element = component.source
      val wrapper = ComponentWrapper(name, component)
      var builder = WebSymbolCodeCompletionItem.create(
        name = name,
        source = wrapper,
        priority = priorityOf(proximity))

      if (proximity == MpxModelVisitor.Proximity.OUT_OF_SCOPE && element != null) {
        val settings = JSApplicationSettings.getInstance()
        if ((scriptLanguage != null && "ts" == scriptLanguage)
            || (DialectDetector.isTypeScript(element)
                && !JSLibraryUtil.isProbableLibraryFile(element.containingFile.viewProvider.virtualFile))) {
          if (settings.hasTSImportCompletionEffective(element.project)) {
            builder = builder.withInsertHandlerAdded(MpxInsertHandler.INSTANCE)
          }
        }
        else {
          if (settings.isUseJavaScriptAutoImport) {
            builder = builder.withInsertHandlerAdded(MpxInsertHandler.INSTANCE)
          }
        }
      }
      return builder
    }
  }

  private class AvailableSlotsContainer(private val tag: XmlTag) : WebSymbolsContainer {

    override fun hashCode(): Int = tag.hashCode()

    override fun equals(other: Any?): Boolean =
      other is AvailableSlotsContainer
      && other.tag == tag

    override fun getModificationCount(): Long = tag.containingFile.modificationStamp

    override fun getSymbols(namespace: Namespace?,
                            kind: SymbolKind,
                            name: String?,
                            params: WebSymbolsNameMatchQueryParams,
                            context: Stack<WebSymbolsContainer>): List<WebSymbolsContainer> =
      if ((namespace == null || namespace == Namespace.HTML) && kind == KIND_MPX_AVAILABLE_SLOTS)
        getAvailableSlots(tag, name, true)
      else emptyList()
  }

  private abstract class DocumentedItemWrapper<T : MpxDocumentedItem>(
    override val matchedName: String, protected val item: T) : MpxWrapperBase(), WebSymbol {

    override val description: String?
      get() = item.documentation.description

    override val docUrl: String?
      get() = item.documentation.docUrl

    override fun equals(other: Any?): Boolean =
      other is DocumentedItemWrapper<*>
      && matchedName == other.matchedName
      && item == other.item

    override fun hashCode(): Int = Objects.hash(matchedName, item)
  }

  private abstract class NamedSymbolWrapper<T : MpxNamedSymbol>(item: T, matchedName: String = item.name,
                                                                override val context: WebSymbolsContainer.Context)
    : DocumentedItemWrapper<T>(matchedName, item) {

    override val name: String
      get() = item.name

    override val source: PsiElement?
      get() = item.source
  }

  private abstract class ScopeElementWrapper<T : MpxDocumentedItem>(matchedName: String, item: T) :
    DocumentedItemWrapper<T>(matchedName, item) {

    override val context: WebSymbolsContainer.Context =
      object : WebSymbolsContainer.Context {

        override val framework: FrameworkId
          get() = VUE_FRAMEWORK

        override val packageName: String?
          get() = (item as MpxScopeElement).parents
            .takeIf { it.size == 1 }
            ?.get(0)
            ?.castSafelyTo<MpxPlugin>()
            ?.moduleName

        override val version: String?
          get() = (item as MpxScopeElement).parents
            .takeIf { it.size == 1 }
            ?.get(0)
            ?.castSafelyTo<MpxPlugin>()
            ?.moduleVersion
      }
  }

  private class ComponentWrapper(matchedName: String, component: MpxComponent) :
    ScopeElementWrapper<MpxComponent>(matchedName, component) {

    override val kind: SymbolKind
      get() = KIND_HTML_VUE_COMPONENTS

    override val name: String
      get() = item.defaultName ?: matchedName

    override val source: PsiElement?
      get() = item.source

    override fun getSymbols(namespace: Namespace?,
                            kind: String,
                            name: String?,
                            params: WebSymbolsNameMatchQueryParams,
                            context: Stack<WebSymbolsContainer>): List<WebSymbolsContainer> =
      if (namespace == null || namespace == Namespace.HTML)
        when (kind) {
          KIND_HTML_VUE_COMPONENT_PROPS -> {
            val searchName = name?.let { fromAsset(it) }
            val props = mutableListOf<MpxInputProperty>()
            item.acceptPropertiesAndMethods(object : MpxModelVisitor() {
              override fun visitInputProperty(prop: MpxInputProperty, proximity: Proximity): Boolean {
                if (searchName == null || fromAsset(prop.name) == searchName) {
                  props.add(prop)
                }
                return true
              }
            })
            props.map { InputPropWrapper(name ?: it.name, it, this.context) }
          }
          KIND_HTML_EVENTS -> {
            (item as? MpxContainer)?.emits?.mapWithNameFilter(name) { EmitCallWrapper(it, this.context) }
            ?: emptyList()
          }
          KIND_HTML_SLOTS -> {
            (item as? MpxContainer)?.slots?.mapWithNameFilter(name) { SlotWrapper(it, this.context) }
            ?: if (!name.isNullOrEmpty()
                   && ((item is MpxContainer && item.template == null)
                       || item is MpxUnresolvedComponent)) {
              listOf(WebSymbolMatch(name, listOf(WebSymbol.NameSegment(0, name.length)), Namespace.HTML, KIND_HTML_SLOTS, this.context))
            }
            else emptyList()
          }
          KIND_MPX_MODEL -> {
            (item as? MpxContainer)?.model?.takeIf {
              it.prop != DEFAULT_PROP || it.event != DEFAULT_EVENT
            }?.let {
              listOf(MpxModelWrapper(this.context, it))
            }
            ?: emptyList()
          }
          else -> emptyList()
        }
      else emptyList()

  }

  private class InputPropWrapper(matchedName: String, property: MpxInputProperty, context: WebSymbolsContainer.Context)
    : NamedSymbolWrapper<MpxInputProperty>(property, matchedName, context) {

    override val kind: SymbolKind
      get() = KIND_HTML_VUE_COMPONENT_PROPS

    override val jsType: JSType?
      get() = item.jsType

    override val required: Boolean
      get() = item.required

    override val attributeValue: WebSymbol.AttributeValue =
      object : WebSymbol.AttributeValue {
        override val default: String?
          get() = item.defaultValue
      }
  }

  private class EmitCallWrapper(emitCall: MpxEmitCall, context: WebSymbolsContainer.Context)
    : NamedSymbolWrapper<MpxEmitCall>(emitCall, context = context) {

    override val kind: SymbolKind
      get() = KIND_HTML_EVENTS

    override val jsType: JSType?
      get() = item.eventJSType
  }

  private class SlotWrapper(slot: MpxSlot, context: WebSymbolsContainer.Context)
    : NamedSymbolWrapper<MpxSlot>(slot, context = context) {

    override val kind: SymbolKind
      get() = KIND_HTML_SLOTS

    override val jsType: JSType?
      get() = item.scope

  }

  private class DirectiveWrapper(matchedName: String, directive: MpxDirective) :
    ScopeElementWrapper<MpxDirective>(matchedName, directive) {

    override val kind: SymbolKind
      get() = KIND_HTML_VUE_DIRECTIVES

    override val name: String
      get() = item.defaultName ?: matchedName

    override val source: PsiElement?
      get() = item.source

    override fun getSymbols(namespace: Namespace?,
                            kind: SymbolKind,
                            name: String?,
                            params: WebSymbolsNameMatchQueryParams,
                            context: Stack<WebSymbolsContainer>): List<WebSymbolsContainer> =
      if ((namespace == null || namespace == Namespace.HTML)
          && (kind == KIND_MPX_DIRECTIVE_ARGUMENT || (name != null && kind == KIND_MPX_DIRECTIVE_MODIFIERS))) {
        listOf(AnyWrapper(this.context, Namespace.HTML, kind, name ?: "Mpx directive argument"))
      }
      else emptyList()

  }

  private class MpxModelWrapper(override val context: WebSymbolsContainer.Context,
                                private val mpxModel: MpxModelDirectiveProperties) : WebSymbol {

    override val namespace: Namespace get() = Namespace.HTML
    override val kind: SymbolKind get() = KIND_MPX_MODEL

    override val properties: Map<String, Any>
      get() = mapOf(
        Pair(PROP_MPX_MODEL_PROP, mpxModel.prop),
        Pair(PROP_MPX_MODEL_EVENT, mpxModel.event),
      )
  }

  private class AnyWrapper(override val context: WebSymbolsContainer.Context,
                           override val namespace: Namespace,
                           override val kind: SymbolKind,
                           override val matchedName: String) : WebSymbol {

  }

}
