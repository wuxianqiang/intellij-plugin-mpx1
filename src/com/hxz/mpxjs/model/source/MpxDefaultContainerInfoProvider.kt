// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.lang.javascript.JSElementTypes
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.types.JSTypeSourceFactory
import com.intellij.lang.javascript.psi.types.evaluable.JSApplyCallType
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.castSafelyTo
import one.util.streamex.StreamEx
import com.hxz.mpxjs.codeInsight.*
import com.hxz.mpxjs.index.*
import com.hxz.mpxjs.model.*
import com.hxz.mpxjs.model.source.MpxComponents.Companion.getComponentDescriptor
import com.hxz.mpxjs.types.MpxSourcePropType

class MpxDefaultContainerInfoProvider : MpxContainerInfoProvider.MpxInitializedContainerInfoProvider(::MpxSourceContainerInfo) {

  private class MpxSourceContainerInfo(declaration: JSElement) : MpxInitializedContainerInfo(declaration) {
    override val data: List<MpxDataProperty> get() = get(DATA)
    override val computed: List<MpxComputedProperty> get() = get(COMPUTED)
    override val methods: List<MpxMethod> get() = get(METHODS)
    override val props: List<MpxInputProperty> get() = get(PROPS)

    override val model: MpxModelDirectiveProperties get() = get(MODEL)

    override val delimiters: Pair<String, String>? get() = get(DELIMITERS)
    override val extends: List<MpxMixin> get() = get(EXTENDS) + get(EXTENDS_CALL)
    override val components: Map<String, MpxComponent> get() = get(COMPONENTS)
    override val directives: Map<String, MpxDirective> get() = get(DIRECTIVES)
    override val mixins: List<MpxMixin> get() = get(MIXINS)
    override val filters: Map<String, MpxFilter> get() = get(FILTERS)

  }

  companion object {

    private val ContainerMember = object {
      val Props: MemberReader = MemberReader(PROPS_PROP, true)
      val Computed = MemberReader(COMPUTED_PROP)
      val Methods = MemberReader(METHODS_PROP)
      val Directives = MemberReader(DIRECTIVES_PROP)
      val Components = MemberReader(COMPONENTS_PROP)
      val Filters = MemberReader(FILTERS_PROP)
      val Delimiters = MemberReader(DELIMITERS_PROP, true, false)
      val Model = MemberReader(MODEL_PROP)
      val Data = MemberReader(DATA_PROP, canBeFunctionResult = true)
    }

    private val EXTENDS = MixinsAccessor(EXTENDS_PROP, MpxExtendsBindingIndex.KEY)
    private val EXTENDS_CALL = ExtendsCallAccessor()
    private val MIXINS = MixinsAccessor(MIXINS_PROP, MpxMixinBindingIndex.KEY)
    private val DIRECTIVES = DirectivesAccessor()
    private val COMPONENTS = ComponentsAccessor()
    private val FILTERS = SimpleMemberMapAccessor(ContainerMember.Filters, ::MpxSourceFilter)
    private val DELIMITERS = DelimitersAccessor()

    private val PROPS = SimpleMemberAccessor(ContainerMember.Props, ::MpxSourceInputProperty)
    private val DATA = SimpleMemberAccessor(ContainerMember.Data, ::MpxSourceDataProperty)
    private val COMPUTED = SimpleMemberAccessor(ContainerMember.Computed, ::MpxSourceComputedProperty)
    private val METHODS = SimpleMemberAccessor(ContainerMember.Methods, ::MpxSourceMethod)

    private val MODEL = ModelAccessor()
  }

  private class ExtendsCallAccessor : ListAccessor<MpxMixin>() {
    override fun build(declaration: JSElement): List<MpxMixin> =
      declaration.context
        ?.let { if (it is JSArgumentList) it.context else it }
        ?.castSafelyTo<JSCallExpression>()
        ?.indexingData
        ?.implicitElements
        ?.asSequence()
        ?.filter { it.userString == MpxExtendsBindingIndex.JS_KEY }
        ?.mapNotNull { MpxComponents.mpxMixinDescriptorFinder(it) }
        ?.mapNotNull { MpxModelManager.getMixin(it) }
        ?.toList()
      ?: emptyList()
  }

  private class MixinsAccessor(private val propertyName: String,
                               private val indexKey: StubIndexKey<String, JSImplicitElementProvider>)
    : ListAccessor<MpxMixin>() {

    override fun build(declaration: JSElement): List<MpxMixin> {
      val mixinsProperty = declaration.castSafelyTo<JSObjectLiteralExpression>()
                             ?.findProperty(propertyName) ?: return emptyList()
      val original = CompletionUtil.getOriginalOrSelf<PsiElement>(mixinsProperty)
      val referencedMixins: List<MpxMixin> =
        resolve(LOCAL, GlobalSearchScope.fileScope(mixinsProperty.containingFile.originalFile), indexKey)
          ?.asSequence()
          ?.filter { PsiTreeUtil.isAncestor(original, it.parent, false) }
          ?.mapNotNull { MpxComponents.mpxMixinDescriptorFinder(it) }
          ?.mapNotNull { MpxModelManager.getMixin(it) }
          ?.toList()
        ?: emptyList()

      val initializerMixins: List<MpxMixin> =
        (mixinsProperty as? StubBasedPsiElement<*>)?.stub
          ?.getChildrenByType(JSElementTypes.OBJECT_LITERAL_EXPRESSION, JSObjectLiteralExpression.ARRAY_FACTORY)
          ?.mapNotNull { MpxModelManager.getMixin(it) }
        ?: (mixinsProperty.value as? JSArrayLiteralExpression)
          ?.expressions
          ?.asSequence()
          ?.filterIsInstance<JSObjectLiteralExpression>()
          ?.mapNotNull { MpxModelManager.getMixin(it) }
          ?.toList()
        ?: emptyList()
      return referencedMixins + initializerMixins
    }
  }

  private class DirectivesAccessor : MapAccessor<MpxDirective>() {
    override fun build(declaration: JSElement): Map<String, MpxDirective> {
      return StreamEx.of(ContainerMember.Directives.readMembers(declaration))
        .mapToEntry({ it.first }, {
          (MpxComponents.meaningfulExpression(it.second) ?: it.second)
            .let { meaningfulElement ->
              objectLiteralFor(meaningfulElement)
              ?: meaningfulElement
            }.let { initializer ->
              @Suppress("USELESS_CAST")
              MpxSourceDirective(it.first, initializer) as MpxDirective
            }
        })
        .distinctKeys()
        .into(mutableMapOf<String, MpxDirective>())
    }
  }

  private class ComponentsAccessor : MapAccessor<MpxComponent>() {
    override fun build(declaration: JSElement): Map<String, MpxComponent> {
      return StreamEx.of(ContainerMember.Components.readMembers(declaration))
        .mapToEntry({ p -> p.first }, { p -> p.second })
        .mapValues { element ->
          when (val meaningfulElement = MpxComponents.meaningfulExpression(element) ?: element) {
            is ES6ImportedBinding ->
              meaningfulElement.declaration?.fromClause
                ?.resolveReferencedElements()
                ?.find { it is JSEmbeddedContent }
                ?.context
                ?.castSafelyTo<XmlTag>()
                ?.takeIf { hasAttribute(it, SETUP_ATTRIBUTE_NAME) }
                ?.containingFile
                ?.let { MpxModelManager.getComponent(it) }
            is HtmlFileImpl ->
              MpxModelManager.getComponent(meaningfulElement)
            else -> getComponentDescriptor(meaningfulElement as? JSElement)
              ?.let { MpxModelManager.getComponent(it) }
          }
          ?: MpxUnresolvedComponent(declaration)
        }
        .distinctKeys()
        .into(mutableMapOf<String, MpxComponent>())
    }
  }

  private class ModelAccessor : MemberAccessor<MpxModelDirectiveProperties>() {
    override fun build(declaration: JSElement): MpxModelDirectiveProperties {
      var prop = MpxModelDirectiveProperties.DEFAULT_PROP
      var event = MpxModelDirectiveProperties.DEFAULT_EVENT
      ContainerMember.Model.readMembers(declaration).forEach { (name, element) ->
        (element as? JSProperty)?.value
          ?.let { getTextIfLiteral(it) }
          ?.let { value ->
            if (name == MODEL_PROP_PROP)
              prop = value
            else if (name == MODEL_EVENT_PROP)
              event = value
          }
      }
      return MpxModelDirectiveProperties(prop, event)
    }
  }

  private class DelimitersAccessor : MemberAccessor<Pair<String, String>?>() {
    override fun build(declaration: JSElement): Pair<String, String>? {
      val delimiters = ContainerMember.Delimiters.readMembers(declaration)
      if (delimiters.size == 2
          && delimiters[0].first.isNotBlank()
          && delimiters[1].first.isNotBlank()) {
        return Pair(delimiters[0].first, delimiters[1].first)
      }
      return null
    }
  }


  private class MpxSourceInputProperty(override val name: String,
                                       sourceElement: PsiElement) : MpxInputProperty {

    override val source: MpxImplicitElement =
      MpxImplicitElement(name, (sourceElement as? JSProperty)?.let { MpxSourcePropType(it) },
                         sourceElement, JSImplicitElement.Type.Property, true)
    override val jsType: JSType? = source.jsType
    override val required: Boolean = getRequiredFromPropOptions((sourceElement as? JSProperty)?.value)
  }

  private class MpxSourceDataProperty(override val name: String,
                                      override val source: PsiElement?) : MpxDataProperty

  private class MpxSourceComputedProperty(override val name: String,
                                          sourceElement: PsiElement) : MpxComputedProperty {
    override val source: MpxImplicitElement
    override val jsType: JSType?

    init {
      var provider = sourceElement
      val returnType = when (sourceElement) {
        is JSFunctionProperty -> sourceElement.returnType
        is JSProperty -> {
          val functionInitializer = sourceElement.tryGetFunctionInitializer()
          if (functionInitializer != null) {
            provider = functionInitializer
            functionInitializer.returnType
          }
          else {
            sourceElement.jsType?.let {
              JSApplyCallType(it, JSTypeSourceFactory.createTypeSource(sourceElement, false))
            }
          }
        }
        else -> null
      }
      source = MpxImplicitElement(name, returnType, provider, JSImplicitElement.Type.Property, true)
      jsType = source.jsType
    }

  }

  private class MpxSourceMethod(override val name: String,
                                override val source: PsiElement?) : MpxMethod {
    override val jsType: JSType? get() = (source as? JSProperty)?.jsType
  }
}
