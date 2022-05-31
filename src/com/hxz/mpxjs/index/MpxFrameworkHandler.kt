// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.index

import com.intellij.lang.ASTNode
import com.intellij.lang.ecmascript6.ES6StubElementTypes
import com.intellij.lang.ecmascript6.psi.*
import com.intellij.lang.javascript.*
import com.intellij.lang.javascript.index.FrameworkIndexingHandler
import com.intellij.lang.javascript.index.JSSymbolUtil
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.ES6Decorator
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList
import com.intellij.lang.javascript.psi.resolve.JSEvaluateContext
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator
import com.intellij.lang.javascript.psi.stubs.JSElementIndexingData
import com.intellij.lang.javascript.psi.stubs.JSImplicitElementStructure
import com.intellij.lang.javascript.psi.stubs.impl.JSElementIndexingDataImpl
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.impl.source.xml.stub.XmlTagStub
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.castSafelyTo
import com.intellij.xml.util.HtmlUtil.SCRIPT_TAG_NAME
import com.hxz.mpxjs.codeInsight.es6Unquote
import com.hxz.mpxjs.codeInsight.getTextIfLiteral
import com.hxz.mpxjs.codeInsight.toAsset
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.libraries.componentDecorator.isComponentDecorator
import com.hxz.mpxjs.model.source.*
import com.hxz.mpxjs.model.source.MpxComponents.Companion.isDefineComponentOrMpxExtendCall
import com.hxz.mpxjs.types.MpxCompositionPropsTypeProvider

class MpxFrameworkHandler : FrameworkIndexingHandler() {
  // 1 here we are just mapping the constants, no lifecycle needed
  // 2 not lazy structure -> no synchronization needed
  // what to do, indexing is done on background thread, component initialization on EDT
  // either we synchronize (but then every access would have penalty)
  // or we are using the static structure
  // here there are only 6 indexes...
  private val MPX_INDEXES = mapOf(
    record(MpxComponentsIndex.KEY),
    record(MpxExtendsBindingIndex.KEY),
    record(MpxGlobalDirectivesIndex.KEY),
    record(MpxMixinBindingIndex.KEY),
    record(MpxOptionsIndex.KEY),
    record(MpxUrlIndex.KEY),
    record(MpxIdIndex.KEY),
    record(MpxGlobalFiltersIndex.KEY)
  )
  private val expectedLiteralOwnerExpressions = TokenSet.create(JSStubElementTypes.CALL_EXPRESSION,
                                                                JSStubElementTypes.NEW_EXPRESSION,
                                                                JSStubElementTypes.ASSIGNMENT_EXPRESSION,
                                                                ES6StubElementTypes.EXPORT_DEFAULT_ASSIGNMENT)


  companion object {
    fun <T : PsiElement> record(key: StubIndexKey<String, T>): Pair<String, StubIndexKey<String, T>> {
      return Pair(MpxIndexBase.createJSKey(key), key)
    }

    private const val REQUIRE = "require"

    private val MPX_DESCRIPTOR_OWNERS = arrayOf(MPX_NAMESPACE, MIXIN_FUN, COMPONENT_FUN, EXTEND_FUN, DIRECTIVE_FUN, DELIMITERS_PROP,
                                                FILTER_FUN, DEFINE_COMPONENT_FUN)
    private val COMPONENT_INDICATOR_PROPS = setOf(TEMPLATE_PROP, DATA_PROP, "render", PROPS_PROP, "propsData", COMPUTED_PROP, METHODS_PROP,
                                                  "watch", MIXINS_PROP, COMPONENTS_PROP, DIRECTIVES_PROP, FILTERS_PROP, SETUP_METHOD,
                                                  MODEL_PROP)

    private val INTERESTING_PROPERTIES = arrayOf(MIXINS_PROP, EXTENDS_PROP, DIRECTIVES_PROP, NAME_PROP, TEMPLATE_PROP)

    fun hasComponentIndicatorProperties(obj: JSObjectLiteralExpression, exclude: String? = null): Boolean =
      obj.properties.any { it.name != exclude && COMPONENT_INDICATOR_PROPS.contains(it.name) }

    fun isDefaultExports(expression: JSExpression?): Boolean =
      expression is JSReferenceExpression && JSSymbolUtil.isAccurateReferenceExpressionName(expression as JSReferenceExpression?,
                                                                                            JSSymbolUtil.EXPORTS, "default")

    fun getExprReferencedFileUrl(expression: JSExpression?): String? {
      if (expression is JSReferenceExpression) {
        for (resolvedElement in resolveLocally(expression)) {
          (resolvedElement as? ES6ImportedBinding)
            ?.declaration
            ?.fromClause
            ?.let {
              return it.referenceText?.let { ref -> StringUtil.unquoteString(ref) }
            }
        }
      }
      else if (expression is JSCallExpression) {
        val referenceExpression = expression.methodExpression as? JSReferenceExpression
        val arguments = expression.arguments
        if (arguments.size == 1
            && arguments[0] is JSLiteralExpression
            && (arguments[0] as JSLiteralExpression).isQuotedLiteral
            && referenceExpression != null
            && referenceExpression.qualifier == null
            && REQUIRE == referenceExpression.referenceName) {
          return (arguments[0] as JSLiteralExpression).stringValue
        }
      }
      return null
    }
  }

  override fun findModule(result: PsiElement): PsiElement? = com.hxz.mpxjs.index.findModule(result)

  override fun interestedProperties(): Array<String> = INTERESTING_PROPERTIES

  override fun processProperty(name: String?, property: JSProperty, out: JSElementIndexingData): Boolean {
    if (MIXINS_PROP == name && property.value is JSArrayLiteralExpression) {
      (property.value as JSArrayLiteralExpression).expressions
        .forEach {
          if (it is JSReferenceExpression) {
            recordMixin(out, property, it, false)
          }
        }
    }
    else if (EXTENDS_PROP == name && property.value is JSReferenceExpression) {
      recordExtends(out, property, property.value)
    }
    //Mpxtify typescript components
    else if (NAME_PROP == name && property.value is JSLiteralExpression) {
      val componentName = (property.value as JSLiteralExpression).stringValue
      val obj = property.parent as JSObjectLiteralExpression
      if (componentName != null && obj.containingFile.name.contains(toAsset(componentName),
                                                                    true) && obj.containingFile.fileType is TypeScriptFileType) {
        out.addImplicitElement(createImplicitElement(componentName, property, MpxComponentsIndex.JS_KEY))
      }
    }
    else if (TEMPLATE_PROP == name) {
      if (isPossiblyMpxContainerInitializer(property.parent as? JSObjectLiteralExpression)) {
        val value = property.value
        if (value is JSLiteralExpression && value.isQuotedLiteral) {
          value.stringValue
            ?.takeIf { it.startsWith("#") && it.length > 1 }
            ?.let {
              JSImplicitElementImpl.Builder(it.substring(1), property)
                .setUserString(this, MpxIdIndex.JS_KEY)
                .forbidAstAccess()
                .toImplicitElement()
            }
            ?.let { out.addImplicitElement(it) }
        }
        else {
          getExprReferencedFileUrl(property.value)
            ?.let { PathUtil.getFileName(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let {
              JSImplicitElementImpl.Builder(it, property)
                .setUserString(this, MpxUrlIndex.JS_KEY)
                .forbidAstAccess()
                .toImplicitElement()
            }
            ?.let { out.addImplicitElement(it) }
        }
      }
    }

    return true
  }

  override fun processAnyProperty(property: JSProperty, outData: JSElementIndexingData?): JSElementIndexingData? {
    val obj = property.parent as? JSObjectLiteralExpression

    var out = outData
    //Bootstrap-mpx components
    if (property.containingFile.name == "index.js" && property.parent is JSObjectLiteralExpression) {
      val parent = PsiTreeUtil.findFirstParent(property, Condition {
        return@Condition it is JSVarStatement && it.variables.firstOrNull()?.name == "components"
      })
      if (parent != null) {
        val componentName = property.name ?: ""
        if (out == null) out = JSElementIndexingDataImpl()
        out.addImplicitElement(createImplicitElement(componentName, property, MpxComponentsIndex.JS_KEY))
      }
    }

    val firstProperty = obj?.firstProperty ?: return outData
    if (firstProperty == property) {
      val parent = obj.parent
      if (parent is JSExportAssignment
          || (parent is JSAssignmentExpression && isDefaultExports(parent.definitionExpression?.expression))
          || parent.castSafelyTo<JSArgumentList>()?.parent
            ?.castSafelyTo<JSCallExpression>()?.let { isDefineComponentOrMpxExtendCall(it) } == true) {
        if (isPossiblyMpxContainerInitializer(obj)) {
          if (out == null) out = JSElementIndexingDataImpl()
          out.addImplicitElement(createImplicitElement(getComponentNameFromDescriptor(obj), property, MpxComponentsIndex.JS_KEY))
        }
      }
      else if (((parent as? JSProperty) == null) && isDescriptorOfLinkedInstanceDefinition(obj)) {
        val binding = (obj.findProperty(EL_PROP)?.value as? JSLiteralExpression)?.stringValue
        if (out == null) out = JSElementIndexingDataImpl()
        out.addImplicitElement(createImplicitElement(binding ?: "", property, MpxOptionsIndex.JS_KEY))
      }
    }

    return out
  }

  override fun processDecorator(decorator: ES6Decorator, data: JSElementIndexingDataImpl?): JSElementIndexingDataImpl? {
    if (!isComponentDecorator(decorator)) return data

    val exportAssignment = (decorator.parent as? JSAttributeList)?.parent as? ES6ExportDefaultAssignment ?: return data
    if (exportAssignment.stubSafeElement !is JSClassExpression) return data

    val nameProperty = MpxComponents.getDescriptorFromDecorator(decorator)?.findProperty(NAME_PROP)
    val name = getTextIfLiteral(nameProperty?.value) ?: FileUtil.getNameWithoutExtension(decorator.containingFile.name)
    val outData = data ?: JSElementIndexingDataImpl()
    outData.addImplicitElement(createImplicitElement(name, decorator, MpxComponentsIndex.JS_KEY, null, null, false))
    return outData
  }

  override fun shouldCreateStubForCallExpression(node: ASTNode?): Boolean {
    val reference = (node?.psi as? JSCallExpression)?.methodExpression as? JSReferenceExpression ?: return false
    return MpxStaticMethod.matchesAny(reference)
  }

  override fun processCallExpression(callExpression: JSCallExpression?, outData: JSElementIndexingData) {
    val reference = callExpression?.methodExpression as? JSReferenceExpression ?: return
    val arguments = callExpression.arguments
    if (arguments.isEmpty()) return

    if (MpxStaticMethod.Component.matches(reference)) {
      if (arguments.size >= 2) {
        var componentName = getTextIfLiteral(arguments[0])
        var nameRefString: String? = null
        if (componentName == null) {
          val nameRef = arguments[0] as? JSReferenceExpression ?: return
          nameRefString = nameRef.text
          val qualifierRef = nameRef.qualifier as? JSReferenceExpression
          componentName = (qualifierRef?.referenceName ?: nameRef.referenceName) + GLOBAL_BINDING_MARK
        }
        outData.addImplicitElement(createImplicitElement(componentName, callExpression, MpxComponentsIndex.JS_KEY,
                                                         nameRefString, arguments[1], true))
      }
    }
    else if (MpxStaticMethod.Mixin.matches(reference)) {
      if (arguments.size == 1) {
        recordMixin(outData, callExpression, arguments[0], true)
      }
    }
    else if (MpxStaticMethod.Directive.matches(reference)) {
      val directiveName = getTextIfLiteral(arguments[0])
      if (arguments.size >= 2 && !directiveName.isNullOrBlank()) {
        recordDirective(outData, callExpression, directiveName, arguments[1])
      }
    }
    else if (MpxStaticMethod.Filter.matches(reference)) {
      val filterName = getTextIfLiteral(arguments[0])
      if (arguments.size >= 2 && !filterName.isNullOrBlank()) {
        val functionDef = arguments[1]
        val nameType = (functionDef as? JSReferenceExpression)?.referenceName
        outData.addImplicitElement(createImplicitElement(
          filterName, callExpression, MpxGlobalFiltersIndex.JS_KEY, nameType,
          arguments[1], true))
      }
    }
    else if (reference.referenceName == EXTEND_FUN) {
      when (val qualifier = reference.qualifier) {
        is JSReferenceExpression -> if (
          !qualifier.hasQualifier() && qualifier.referenceName != MPX_NAMESPACE) {
          recordExtends(outData, callExpression, reference.qualifier)
        }
        // 3-rd party library support: mpx-typed-mixin
        is JSCallExpression -> {
          val mixinsCall = qualifier.methodExpression?.castSafelyTo<JSReferenceExpression>()
            ?.takeIf { !it.hasQualifier() }
          if (mixinsCall?.referenceName != null
              && JSStubBasedPsiTreeUtil.resolveLocally(mixinsCall.referenceName!!, mixinsCall)
                ?.castSafelyTo<ES6ImportedBinding>()
                ?.context?.castSafelyTo<ES6ImportExportDeclaration>()
                ?.fromClause
                ?.referenceText
                ?.let { es6Unquote(it) } == "mpx-typed-mixins") {
            for (arg in qualifier.arguments) {
              arg.castSafelyTo<JSReferenceExpression>()
                ?.takeIf { !it.hasQualifier() }
                ?.let { recordExtends(outData, callExpression, it) }
            }
          }
        }
      }
    }
  }

  private fun recordDirective(outData: JSElementIndexingData,
                              provider: JSImplicitElementProvider,
                              directiveName: String,
                              descriptorRef: PsiElement?) {
    outData.addImplicitElement(createImplicitElement(directiveName, provider, MpxGlobalDirectivesIndex.JS_KEY,
                                                     null, descriptorRef, true))
  }

  private fun recordMixin(outData: JSElementIndexingData,
                          provider: JSImplicitElementProvider,
                          descriptorRef: PsiElement?,
                          isGlobal: Boolean) {
    outData.addImplicitElement(createImplicitElement(if (isGlobal) GLOBAL else LOCAL, provider, MpxMixinBindingIndex.JS_KEY, null,
                                                     descriptorRef, isGlobal))
  }

  private fun recordExtends(outData: JSElementIndexingData,
                            provider: JSImplicitElementProvider,
                            descriptorRef: PsiElement?) {
    outData.addImplicitElement(createImplicitElement(LOCAL, provider, MpxExtendsBindingIndex.JS_KEY, null,
                                                     descriptorRef, false))
  }

  private fun isPossiblyMpxContainerInitializer(initializer: JSObjectLiteralExpression?): Boolean {
    return initializer != null
           && (initializer.containingFile.fileType == MpxFileType.INSTANCE
               || (initializer.containingFile is JSFile && hasComponentIndicatorProperties(initializer)))
  }

  private fun isDescriptorOfLinkedInstanceDefinition(obj: JSObjectLiteralExpression): Boolean {
    val argumentList = obj.parent as? JSArgumentList ?: return false
    if (argumentList.arguments[0] == obj) {
      return JSSymbolUtil.isAccurateReferenceExpressionName(
        (argumentList.parent as? JSNewExpression)?.methodExpression as? JSReferenceExpression, MPX_NAMESPACE) ||
             JSSymbolUtil.isAccurateReferenceExpressionName(
               (argumentList.parent as? JSCallExpression)?.methodExpression as? JSReferenceExpression, MPX_NAMESPACE, EXTEND_FUN)
    }
    return false
  }

  override fun addTypeFromResolveResult(evaluator: JSTypeEvaluator, context: JSEvaluateContext, result: PsiElement): Boolean =
    MpxCompositionPropsTypeProvider.addTypeFromResolveResult(evaluator, context, result)

  override fun useOnlyCompleteMatch(type: JSType, evaluateContext: JSEvaluateContext): Boolean =
    MpxCompositionPropsTypeProvider.useOnlyCompleteMatch(type, evaluateContext)

  override fun shouldCreateStubForLiteral(node: ASTNode?): Boolean {
    if (node?.psi is JSLiteralExpression) {
      return hasSignificantValue(node.psi as JSLiteralExpression)
    }
    return super.shouldCreateStubForLiteral(node)
  }

  override fun hasSignificantValue(expression: JSLiteralExpression): Boolean {
    val parentType = expression.node.treeParent?.elementType ?: return false
    if (JSElementTypes.ARRAY_LITERAL_EXPRESSION == parentType
        || (JSElementTypes.PROPERTY == parentType
            && expression.node.treeParent.findChildByType(JSTokenTypes.IDENTIFIER)?.text in listOf(PROPS_REQUIRED_PROP, EL_PROP))) {
      return MpxFileType.INSTANCE == expression.containingFile.fileType || insideMpxDescriptor(expression)
    }
    return false
  }

  // limit building stub in other file types like js/html to Mpx-descriptor-like members
  private fun insideMpxDescriptor(expression: JSLiteralExpression): Boolean {
    val statement = TreeUtil.findParent(expression.node,
                                        expectedLiteralOwnerExpressions,
                                        JSExtendedLanguagesTokenSetProvider.STATEMENTS) ?: return false
    if (statement.elementType == ES6StubElementTypes.EXPORT_DEFAULT_ASSIGNMENT) return true
    val referenceHolder = if (statement.elementType == JSStubElementTypes.ASSIGNMENT_EXPRESSION)
      statement.findChildByType(JSStubElementTypes.DEFINITION_EXPRESSION)
    else statement
    val ref = referenceHolder?.findChildByType(JSElementTypes.REFERENCE_EXPRESSION) ?: return false
    return ref.getChildren(JSKeywordSets.IDENTIFIER_NAMES).filter { it.text in MPX_DESCRIPTOR_OWNERS }.any()
  }

  private fun getComponentNameFromDescriptor(obj: JSObjectLiteralExpression): String {
    return ((obj.findProperty(NAME_PROP)?.value as? JSLiteralExpression)?.stringValue
            ?: FileUtil.getNameWithoutExtension(obj.containingFile.name))
  }

  override fun indexImplicitElement(element: JSImplicitElementStructure, sink: IndexSink?): Boolean {
    val index = MPX_INDEXES[element.userString]
    if (index != null) {
      sink?.occurrence(index, element.name)
    }
    return index == MpxUrlIndex.KEY
  }

  private fun createImplicitElement(name: String, provider: PsiElement, indexKey: String,
                                    nameType: String? = null,
                                    descriptor: PsiElement? = null,
                                    isGlobal: Boolean = false): JSImplicitElementImpl {
    val normalized = normalizeNameForIndex(name)
    val nameTypeRecord = nameType ?: ""
    val asIndexed = descriptor as? JSIndexedPropertyAccessExpression
    var descriptorRef = asIndexed?.qualifier?.text ?: (descriptor as? JSReferenceExpression)?.text ?: ""
    if (asIndexed != null) descriptorRef += INDEXED_ACCESS_HINT
    return JSImplicitElementImpl.Builder(normalized, provider)
      .setUserString(this, indexKey)
      .setTypeString("${if (isGlobal) 1 else 0}$DELIMITER$nameTypeRecord$DELIMITER$descriptorRef$DELIMITER$name")
      .toImplicitElement()
  }

  override fun computeJSImplicitElementUserStringKeys(): Set<String> {
    return setOf(MpxUrlIndex.JS_KEY, MpxOptionsIndex.JS_KEY, MpxMixinBindingIndex.JS_KEY, MpxComponentsIndex.JS_KEY,
                 MpxGlobalDirectivesIndex.JS_KEY, MpxExtendsBindingIndex.JS_KEY, MpxGlobalFiltersIndex.JS_KEY, MpxIdIndex.JS_KEY)
  }
}

fun resolveLocally(ref: JSReferenceExpression): List<PsiElement> {
  return if (ref.qualifier == null && ref.referenceName != null) {
    JSStubBasedPsiTreeUtil.resolveLocallyWithMergedResults(ref.referenceName!!, ref)
  }
  else emptyList()
}

@StubSafe
fun findModule(element: PsiElement?): JSEmbeddedContent? =
  (element as? XmlFile ?: element?.containingFile as? XmlFile)
    ?.let { findScriptTag(it) }
    ?.let { PsiTreeUtil.getStubChildOfType(it, JSEmbeddedContent::class.java) }

@StubSafe
fun findScriptTag(xmlFile: XmlFile): XmlTag? =
  findTopLevelMpxTag(xmlFile, SCRIPT_TAG_NAME)

@StubSafe
fun findAttribute(tag: XmlTag, attributeName: String): XmlAttribute? =
  PsiTreeUtil.getStubChildrenOfTypeAsList(tag, XmlAttribute::class.java).firstOrNull { it.name == attributeName }

@StubSafe
fun hasAttribute(tag: XmlTag, attributeName: String): Boolean =
  PsiTreeUtil.getStubChildrenOfTypeAsList(tag, XmlAttribute::class.java).any { it.name == attributeName }

@StubSafe
fun findTopLevelMpxTag(xmlFile: XmlFile, tagName: String): XmlTag? {
  if (xmlFile.fileType == MpxFileType.INSTANCE) {
    var result: XmlTag? = null
    if (xmlFile is PsiFileImpl) {
      xmlFile.stub?.let { stub ->
        return stub.childrenStubs
          .asSequence()
          .mapNotNull { (it as? XmlTagStub<*>)?.psi }
          .find { it.localName.equals(tagName, ignoreCase = true) }
      }
    }

    xmlFile.accept(object : MpxFileVisitor() {
      override fun visitXmlTag(tag: XmlTag?) {
        if (result == null
            && tag != null
            && tag.localName.equals(tagName, ignoreCase = true)) {
          result = tag
        }
      }
    })
    return result
  }
  return null
}

fun findTopLevelMpxTags(xmlFile: XmlFile, tagName: String): List<XmlTag> {
  if (xmlFile.fileType == MpxFileType.INSTANCE) {
    if (xmlFile is PsiFileImpl) {
      xmlFile.stub?.let { stub ->
        return stub.childrenStubs
          .asSequence()
          .mapNotNull { (it as? XmlTagStub<*>)?.psi }
          .filter { it.localName.equals(tagName, ignoreCase = true) }
          .toList()
      }
    }
    val result = SmartList<XmlTag>()
    xmlFile.accept(object : MpxFileVisitor() {
      override fun visitXmlTag(tag: XmlTag?) {
        if (tag != null
            && tag.localName.equals(tagName, ignoreCase = true)) {
          result.add(tag)
        }
      }
    })
    return result
  }
  return emptyList()
}

private enum class MpxStaticMethod(val methodName: String) {
  Component(COMPONENT_FUN),
  Mixin(MIXIN_FUN),
  Directive(DIRECTIVE_FUN),
  Filter(FILTER_FUN);

  companion object {
    fun matchesAny(reference: JSReferenceExpression): Boolean = values().any { it.matches(reference) }
  }

  fun matches(reference: JSReferenceExpression): Boolean =
    JSSymbolUtil.isAccurateReferenceExpressionName(reference, MPX_NAMESPACE, methodName)
}

open class MpxFileVisitor : XmlElementVisitor() {
  override fun visitXmlDocument(document: XmlDocument?): Unit = recursion(document)

  override fun visitXmlFile(file: XmlFile?): Unit = recursion(file)

  protected fun recursion(element: PsiElement?) {
    element?.children?.forEach { it.accept(this) }
  }
}
