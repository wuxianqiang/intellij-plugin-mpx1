// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.editor

import com.intellij.codeInsight.hints.Option
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.lang.javascript.editing.JavaScriptInlayParameterHintsProvider
import com.intellij.lang.javascript.psi.JSCallLikeExpression
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterExpression

class MpxJSInlayParameterHintsProvider : JavaScriptInlayParameterHintsProvider() {

  override fun getShowNameForAllArgsOption(): Option {
    return NAMES_FOR_ALL_ARGS
  }

  override fun getSupportedOptions(): List<Option> {
    return listOf(showNameForAllArgsOption, NAMES_FOR_FILTERS)
  }

  override fun isSuitableCallExpression(expression: JSCallLikeExpression?): Boolean {
    return super.isSuitableCallExpression(expression)
           && (NAMES_FOR_FILTERS.get() || expression !is MpxJSFilterExpression)
  }

  override fun skipIndex(i: Int, expression: JSCallLikeExpression): Boolean {
    return if (expression is MpxJSFilterExpression && i == 0) true
    else super.skipIndex(i, expression)
  }

  companion object {
    val NAMES_FOR_ALL_ARGS = Option(
      "mpxjs.show.names.for.all.args", JavaScriptBundle.messagePointer("js.param.hints.show.names.for.all.args"), false)
    val NAMES_FOR_FILTERS = Option(
      "mpxjs.show.names.for.filters", MpxBundle.messagePointer("mpx.param.hints.show.names.for.filters"), true)
  }
}
