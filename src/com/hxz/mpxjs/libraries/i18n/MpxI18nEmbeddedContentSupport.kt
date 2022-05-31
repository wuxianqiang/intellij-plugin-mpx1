// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.libraries.i18n

import com.intellij.html.embedding.HtmlEmbeddedContentProvider
import com.intellij.html.embedding.HtmlEmbeddedContentSupport
import com.intellij.html.embedding.HtmlEmbedmentInfo
import com.intellij.html.embedding.HtmlTagEmbeddedContentProvider
import com.intellij.lexer.BaseHtmlLexer
import com.hxz.mpxjs.lang.html.lexer.MpxLexer

class MpxI18nEmbeddedContentSupport: HtmlEmbeddedContentSupport {

  override fun isEnabled(lexer: BaseHtmlLexer): Boolean = lexer is MpxLexer

  override fun createEmbeddedContentProviders(lexer: BaseHtmlLexer): List<HtmlEmbeddedContentProvider> =
    listOf( MpxI18nTagEmbeddedContentProvider(lexer) )

  class MpxI18nTagEmbeddedContentProvider(lexer: BaseHtmlLexer) : HtmlTagEmbeddedContentProvider(lexer) {

    override fun isInterestedInTag(tagName: CharSequence): Boolean = namesEqual(tagName, "i18n")

    override fun isInterestedInAttribute(attributeName: CharSequence): Boolean = true

    override fun createEmbedmentInfo(): HtmlEmbedmentInfo? =
      if (attributeName == null) HtmlEmbeddedContentProvider.RAW_TEXT_EMBEDMENT else null
  }
}