// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.lang.javascript.completion.JSCompletionUtil
import com.intellij.lang.javascript.completion.JSLookupPriority
import com.intellij.lang.javascript.completion.JSLookupPriority.*
import com.intellij.lang.javascript.completion.JSLookupUtilImpl
import com.intellij.lang.javascript.psi.JSPsiElementBase
import com.intellij.lang.javascript.psi.JSThisExpression
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.util.ProcessingContext
import com.intellij.util.Processor
import com.hxz.mpxjs.codeInsight.template.MpxTemplateScopesResolver
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterReferenceExpression
import com.hxz.mpxjs.model.MpxFilter
import com.hxz.mpxjs.model.MpxModelManager
import com.hxz.mpxjs.model.MpxModelVisitor

class MpxJSCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    var ref = parameters.position.containingFile.findReferenceAt(parameters.offset)
    if (ref is PsiMultiReference) {
      ref = ref.references.find { r -> r is MpxJSFilterReferenceExpression || r is JSReferenceExpressionImpl }
    }
    if (ref is MpxJSFilterReferenceExpression) {
      val container = MpxModelManager.findEnclosingContainer(ref)
      container?.acceptEntities(object : MpxModelVisitor() {
        override fun visitFilter(name: String, filter: MpxFilter, proximity: Proximity): Boolean {
          if (proximity !== Proximity.OUT_OF_SCOPE) {
            (filter.source
             ?: JSImplicitElementImpl.Builder(name, ref).setType(JSImplicitElement.Type.Method).forbidAstAccess().toImplicitElement())
              .let { JSLookupUtilImpl.createLookupElement(it, name) }
              .let { JSCompletionUtil.withJSLookupPriority(it, getJSLookupPriorityOf(proximity)) }
              .let { result.consume(it) }
          }
          return proximity !== Proximity.OUT_OF_SCOPE
        }
      })
      result.stopHere()
    }
    else if (ref is JSReferenceExpressionImpl && ref.qualifier is JSThisExpression?) {
      MpxTemplateScopesResolver.resolve(ref, Processor { resolveResult ->
        val element = resolveResult.element as? JSPsiElementBase
        if (element != null) {
          result.consume(JSCompletionUtil.withJSLookupPriority(JSLookupUtilImpl.createLookupElement(element),
                                                               if (element.name?.startsWith("$") == true)
                                                                 LOCAL_SCOPE_MAX_PRIORITY_EXOTIC
                                                               else
                                                                 LOCAL_SCOPE_MAX_PRIORITY))
        }
        true
      })
    }
  }

  private fun getJSLookupPriorityOf(proximity: MpxModelVisitor.Proximity): JSLookupPriority =
    when (proximity) {
      MpxModelVisitor.Proximity.LOCAL -> LOCAL_SCOPE_MAX_PRIORITY
      MpxModelVisitor.Proximity.APP -> NESTING_LEVEL_1
      MpxModelVisitor.Proximity.PLUGIN -> NESTING_LEVEL_2
      MpxModelVisitor.Proximity.GLOBAL -> NESTING_LEVEL_3
      else -> LOWEST_PRIORITY
    }
}
