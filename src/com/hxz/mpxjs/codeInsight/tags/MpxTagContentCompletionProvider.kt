// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.tags

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.html.HtmlTag
import com.intellij.util.ProcessingContext
import com.hxz.mpxjs.MpxjsIcons
import com.hxz.mpxjs.codeInsight.completion.mpxtify.MpxtifyIcons
import com.hxz.mpxjs.context.isMpxContext

class MpxTagContentCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    if (!isMpxContext(parameters.position)) return
    if ((parameters.position.parent.parent as? HtmlTag)?.name?.contains("v-icon") == true) {
      MpxtifyIcons.materialAndFontAwesome.forEach {
        result.addElement(LookupElementBuilder.create(it).withIcon(MpxjsIcons.Mpx))
      }
    }
  }

}
