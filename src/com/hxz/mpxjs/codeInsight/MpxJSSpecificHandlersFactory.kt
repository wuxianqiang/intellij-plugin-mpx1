// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.lang.javascript.JavaScriptSpecificHandlersFactory
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.resolve.JSEvaluateContext
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.hxz.mpxjs.codeInsight.refs.MpxJSReferenceExpressionResolver

class MpxJSSpecificHandlersFactory : JavaScriptSpecificHandlersFactory() {
  override fun createReferenceExpressionResolver(referenceExpression: JSReferenceExpressionImpl?,
                                                 ignorePerformanceLimits: Boolean): ResolveCache.PolyVariantResolver<JSReferenceExpressionImpl> =
    MpxJSReferenceExpressionResolver(referenceExpression, ignorePerformanceLimits)

  override fun newTypeEvaluator(context: JSEvaluateContext): JSTypeEvaluator =
    MpxJSTypeEvaluator(context)
}

