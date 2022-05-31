// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.highlighting

import com.intellij.html.embedding.HtmlEmbeddedContentProvider
import com.intellij.lang.javascript.dialects.JSLanguageLevel
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.HtmlHighlightingLexer
import com.intellij.lexer.XHtmlHighlightingLexer
import com.intellij.lexer.HtmlScriptStyleEmbeddedContentProvider
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.xml.XmlTokenType
import com.intellij.psi.xml.XmlTokenType.XML_REAL_WHITE_SPACE
import com.hxz.mpxjs.lang.html.lexer.MpxLexer
import com.hxz.mpxjs.lang.html.lexer.MpxLexerImpl
import com.hxz.mpxjs.lang.html.lexer.MpxLexerImpl.MpxMergingLexer.Companion.getBaseLexerState
import com.hxz.mpxjs.lang.html.lexer.MpxLexerImpl.MpxMergingLexer.Companion.isLexerWithinUnterminatedInterpolation
import com.hxz.mpxjs.lang.html.lexer._MpxLexer

class MpxHighlightingLexer(override val languageLevel: JSLanguageLevel,
                           override val project: Project?,
                           override val interpolationConfig: Pair<String, String>?)
  : HtmlHighlightingLexer(MpxHighlightingMergingLexer(FlexAdapter(
  _MpxLexer(interpolationConfig))),
                          true, null), MpxLexer {

  override fun acceptEmbeddedContentProvider(provider: HtmlEmbeddedContentProvider): Boolean {
    return provider !is HtmlScriptStyleEmbeddedContentProvider
  }

  override fun isHtmlTagState(state: Int): Boolean {
    return state == _MpxLexer.START_TAG_NAME || state == _MpxLexer.END_TAG_NAME
  }

  override fun createAttributeEmbedmentTokenSet(): TokenSet {
    return TokenSet.orSet(super.createAttributeEmbedmentTokenSet(), MpxLexerImpl.ATTRIBUTE_TOKENS)
  }

  override fun createTagEmbedmentStartTokenSet(): TokenSet {
    return TokenSet.orSet(super.createTagEmbedmentStartTokenSet(), MpxLexerImpl.TAG_TOKENS)
  }

  override fun getTokenType(): IElementType? {
    val type = super.getTokenType()
    if ((type === XmlTokenType.TAG_WHITE_SPACE
         && (getBaseLexerState(state) == 0 || isLexerWithinUnterminatedInterpolation(state)))) {
      return XML_REAL_WHITE_SPACE
    }
    return type
  }

  private class MpxHighlightingMergingLexer constructor(original: FlexAdapter)
    : MpxLexerImpl.MpxMergingLexer(original) {

    override fun merge(type: IElementType?, originalLexer: Lexer): IElementType? {
      val tokenType = super.merge(type, originalLexer)
      if (tokenType === XmlTokenType.XML_CHAR_ENTITY_REF) {
        while (originalLexer.tokenType === XmlTokenType.XML_CHAR_ENTITY_REF) {
          originalLexer.advance()
        }
        if (originalLexer.tokenType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
          return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
        }
      }
      return tokenType
    }
  }

}

