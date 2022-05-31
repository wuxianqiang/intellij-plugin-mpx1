// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.types

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.resolve.JSEvaluateContext
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.model.MpxModelManager
import com.hxz.mpxjs.model.source.SETUP_METHOD

object MpxCompositionPropsTypeProvider {
  fun addTypeFromResolveResult(evaluator: JSTypeEvaluator, context: JSEvaluateContext, result: PsiElement): Boolean {
    if (result is JSParameter && PsiTreeUtil.getStubChildOfType(result.context, JSParameter::class.java) == result) {
      val method = result.context?.let { (it as? JSParameterList)?.context ?: it }?.castSafelyTo<JSFunction>() ?: return false
      val initializer = method.context
      if (method.name != SETUP_METHOD
          || initializer !is JSObjectLiteralExpression
          || PsiTreeUtil.getContextOfType(initializer, true, JSClass::class.java, JSObjectLiteralExpression::class.java) != null
          || !isMpxContext(method)
      ) return false
      MpxModelManager.getComponent(initializer)
        ?.thisType?.asRecordType()
        ?.findPropertySignature("\$props")
        ?.jsType
        ?.let {
          evaluator.addType(it)
          return true
        }
    }
    return false
  }

  fun useOnlyCompleteMatch(type: JSType, evaluateContext: JSEvaluateContext): Boolean = type is MpxCompleteType
}