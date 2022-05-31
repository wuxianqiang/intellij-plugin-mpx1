// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.refs

import com.intellij.lang.javascript.patterns.JSPatterns
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSThisExpression
import com.intellij.lang.javascript.psi.impl.JSPropertyImpl
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.resolve.CachingPolyReferenceBase
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.codeInsight.getTextIfLiteral
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.index.MpxIdIndex
import com.hxz.mpxjs.model.MpxModelManager
import com.hxz.mpxjs.model.source.NAME_PROP
import com.hxz.mpxjs.model.source.TEMPLATE_PROP
import com.hxz.mpxjs.model.source.MpxSourceEntity

class MpxJSReferenceContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(THIS_INSIDE_COMPONENT, MpxComponentLocalReferenceProvider())
    registrar.registerReferenceProvider(COMPONENT_NAME, MpxComponentNameReferenceProvider())
    registrar.registerReferenceProvider(TEMPLATE_ID_REF, MpxTemplateIdReferenceProvider())
  }

  companion object {
    private val THIS_INSIDE_COMPONENT: ElementPattern<out PsiElement> = createThisInsideComponentPattern()
    private val COMPONENT_NAME: ElementPattern<out PsiElement> = createComponentNamePattern()
    private val TEMPLATE_ID_REF = JSPatterns.jsLiteral()
      .withParent(JSPatterns.jsProperty().withName(TEMPLATE_PROP))

    private fun createThisInsideComponentPattern(): ElementPattern<out PsiElement> {
      return PlatformPatterns.psiElement(JSReferenceExpression::class.java)
        .and(FilterPattern(object : ElementFilter {
          override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
            return MpxModelManager.findComponentForThisResolve(
              element.castSafelyTo<JSReferenceExpression>()?.qualifier?.castSafelyTo() ?: return false) != null
          }

          override fun isClassAcceptable(hintClass: Class<*>?): Boolean {
            return true
          }
        }))
    }

    private fun createComponentNamePattern(): ElementPattern<out PsiElement> {
      return PlatformPatterns.psiElement(JSLiteralExpression::class.java)
        .withParent(JSPatterns.jsProperty().withName(NAME_PROP))
        .and(FilterPattern(object : ElementFilter {
          override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
            if (element !is JSElement) return false
            val component = MpxModelManager.findEnclosingComponent(element) as? MpxSourceEntity ?: return false
            return component.initializer == element.parent?.parent
          }

          override fun isClassAcceptable(hintClass: Class<*>?): Boolean {
            return true
          }

        }))
    }
  }


  private class MpxTemplateIdReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
      return if (getTextIfLiteral(element)?.startsWith("#") == true
                 && isMpxContext(element)) {
        arrayOf(MpxTemplateIdReference(element as JSLiteralExpression, TextRange(2, element.textLength - 1)))
      }
      else emptyArray()
    }
  }

  private class MpxComponentLocalReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
      if (element is JSReferenceExpressionImpl) {
        return arrayOf(MpxComponentLocalReference(element, ElementManipulators.getValueTextRange(element)))
      }
      return emptyArray()
    }
  }


  private class MpxComponentLocalReference(reference: JSReferenceExpressionImpl,
                                           textRange: TextRange?)
    : CachingPolyReferenceBase<JSReferenceExpressionImpl>(reference, textRange) {

    override fun resolveInner(): Array<ResolveResult> {
      val ref = element
      val name = ref.referenceName
      if (name == null) return ResolveResult.EMPTY_ARRAY
      return ref.qualifier
               .castSafelyTo<JSThisExpression>()
               ?.let { MpxModelManager.findComponentForThisResolve(it) }
               ?.thisType
               ?.asRecordType()
               ?.findPropertySignature(name)
               ?.memberSource
               ?.allSourceElements
               ?.mapNotNull { if (it.isValid) PsiElementResolveResult(it) else null }
               ?.toTypedArray<ResolveResult>()
             ?: ResolveResult.EMPTY_ARRAY
    }
  }

  private class MpxComponentNameReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
      if (element is JSLiteralExpression) {
        return arrayOf(MpxComponentNameReference(element, ElementManipulators.getValueTextRange(element)))
      }
      return emptyArray()
    }

  }

  private class MpxComponentNameReference(element: JSLiteralExpression,
                                          rangeInElement: TextRange?) : CachingPolyReferenceBase<JSLiteralExpression>(element,
                                                                                                                      rangeInElement) {
    override fun resolveInner(): Array<ResolveResult> {
      getParentOfType(element, JSPropertyImpl::class.java, true) ?: return emptyArray()
      return arrayOf(PsiElementResolveResult(JSImplicitElementImpl(element.value.toString(), element)))
    }
  }

  private class MpxTemplateIdReference(element: JSLiteralExpression, rangeInElement: TextRange?)
    : CachingPolyReferenceBase<JSLiteralExpression>(element, rangeInElement) {
    override fun resolveInner(): Array<ResolveResult> {
      val result = mutableListOf<ResolveResult>()
      StubIndex.getInstance().processElements(MpxIdIndex.KEY, value, element.project,
                                              GlobalSearchScope.projectScope(element.project),
                                              PsiElement::class.java) { element ->
        (element as? XmlAttribute)
          ?.context
          ?.castSafelyTo<XmlTag>()
          ?.let { result.add(PsiElementResolveResult(it)) }
        true
      }
      return result.toTypedArray()
    }
  }

}
