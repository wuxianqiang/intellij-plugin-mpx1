// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.template

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.lang.javascript.psi.JSPsiElementBase
import com.intellij.lang.javascript.psi.resolve.JSResolveResult
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveResult
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ObjectUtils.notNull
import com.intellij.util.containers.Stack
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.*
import com.hxz.mpxjs.codeInsight.findExpressionInAttributeValue
import com.hxz.mpxjs.lang.expr.psi.MpxJSSlotPropsExpression
import com.hxz.mpxjs.lang.expr.psi.MpxJSVForExpression
import java.util.function.Consumer

class MpxTemplateElementsScopeProvider : MpxTemplateScopesProvider() {

  override fun getScopes(element: PsiElement, hostElement: PsiElement?): List<MpxTemplateScope> {
    val hostFile = CompletionUtil.getOriginalOrSelf(hostElement ?: element).containingFile
    val templateRootScope = CachedValuesManager.getCachedValue(hostFile) {
      CachedValueProvider.Result.create(
        MpxTemplateScopeBuilder(hostFile).topLevelScope,
        PsiModificationTracker.MODIFICATION_COUNT)
    }
    return listOf(templateRootScope.findBestMatchingTemplateScope(notNull(hostElement, element))!!)
  }

  private class MpxTemplateElementScope constructor(root: PsiElement,
                                                    parent: MpxTemplateElementScope?) : MpxTemplateScope(parent) {

    private val elements = ArrayList<JSPsiElementBase>()

    private val myRange: TextRange = root.textRange

    init {
      if (parent != null) {
        assert(parent.myRange.contains(myRange))
      }
    }

    override fun resolve(consumer: Consumer<in ResolveResult>) {
      elements.forEach { el -> consumer.accept(JSResolveResult(el)) }
    }

    fun add(element: JSPsiElementBase) {
      elements.add(element)
    }

    fun findBestMatchingTemplateScope(element: PsiElement): MpxTemplateElementScope? {
      if (!myRange.contains(element.textOffset)) {
        return null
      }
      var curScope: MpxTemplateElementScope? = null
      var innerScope: MpxTemplateElementScope? = this
      while (innerScope != null) {
        curScope = innerScope
        innerScope = null
        for (child in curScope.children) {
          if (child is MpxTemplateElementScope && child.myRange.contains(element.textOffset)) {
            innerScope = child
            break
          }
        }
      }
      return curScope
    }
  }

  private open class MpxBaseScopeBuilder constructor(private val myTemplateFile: PsiFile) : XmlRecursiveElementVisitor() {
    private val scopes = Stack<MpxTemplateElementScope>()

    val topLevelScope: MpxTemplateElementScope
      get() {
        myTemplateFile.accept(this)
        assert(scopes.size == 1)
        return scopes.peek()
      }

    init {
      scopes.add(MpxTemplateElementScope(myTemplateFile, null))
    }

    fun currentScope(): MpxTemplateElementScope {
      return scopes.peek()
    }

    fun popScope() {
      scopes.pop()
    }

    fun pushScope(tag: XmlTag) {
      scopes.push(MpxTemplateElementScope(tag, currentScope()))
    }

    fun addElement(element: JSPsiElementBase) {
      currentScope().add(element)
    }
  }

  private class MpxTemplateScopeBuilder constructor(templateFile: PsiFile) : MpxBaseScopeBuilder(templateFile) {

    override fun visitXmlTag(tag: XmlTag) {
      val tagHasVariables = tag.attributes
        .any { attribute ->
          attribute
            ?.let { MpxAttributeNameParser.parse(it.name, it.parent) }
            ?.let { info ->
              info.kind === MpxAttributeKind.SLOT_SCOPE
              || info.kind === MpxAttributeKind.SCOPE
              || (info as? MpxDirectiveInfo)?.directiveKind?.let {
                it === MpxDirectiveKind.FOR
                || (it === MpxDirectiveKind.SLOT && attribute.value != null)
              } ?: false
            }
          ?: false
        }

      if (tagHasVariables) {
        pushScope(tag)
      }
      super.visitXmlTag(tag)
      if (tagHasVariables) {
        popScope()
      }
    }

    override fun visitXmlAttribute(attribute: XmlAttribute?) {
      attribute
        ?.let { MpxAttributeNameParser.parse(it.name, it.parent) }
        ?.let { info ->
          when (info.kind) {
            MpxAttributeKind.SLOT_SCOPE -> addSlotProps(attribute)
            MpxAttributeKind.SCOPE -> addSlotProps(attribute)
            MpxAttributeKind.DIRECTIVE ->
              when ((info as MpxDirectiveInfo).directiveKind) {
                MpxDirectiveKind.FOR -> addVForVariables(attribute)
                MpxDirectiveKind.SLOT -> addSlotProps(attribute)
                else -> {
                }
              }
            else -> {
            }
          }
        }
    }

    private fun addSlotProps(attribute: XmlAttribute) {
      findExpressionInAttributeValue(attribute, MpxJSSlotPropsExpression::class.java)
        ?.getParameterList()
        ?.parameterVariables
        ?.forEach { addElement(it) }
    }

    private fun addVForVariables(attribute: XmlAttribute) {
      findExpressionInAttributeValue(attribute, MpxJSVForExpression::class.java)
        ?.getVarStatement()
        ?.variables
        ?.forEach { addElement(it) }
    }
  }
}
