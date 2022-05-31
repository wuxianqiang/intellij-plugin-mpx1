// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.highlighting

import com.intellij.ide.highlighter.HtmlFileHighlighter
import com.intellij.lang.javascript.JSKeywordSets
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.dialects.JSLanguageLevel
import com.intellij.lang.javascript.highlighting.JSHighlighter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.XmlHighlighterColors.HTML_CODE
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair.pair
import com.intellij.psi.tree.IElementType
import com.hxz.mpxjs.lang.html.lexer.MpxTokenTypes.Companion.INTERPOLATION_END
import com.hxz.mpxjs.lang.html.lexer.MpxTokenTypes.Companion.INTERPOLATION_START

internal class MpxFileHighlighter(private val languageLevel: JSLanguageLevel,
                                  private val project: Project?,
                                  private val myInterpolationConfig: Pair<String, String>?) : HtmlFileHighlighter() {

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    keys[tokenType]?.let { return it }
    return super.getTokenHighlights(tokenType)
  }

  override fun getHighlightingLexer(): Lexer {
    return MpxHighlightingLexer(languageLevel, project, myInterpolationConfig)
  }

  companion object {

    private val MPX_INTERPOLATION_DELIMITER = TextAttributesKey.createTextAttributesKey(
      "MPX.SCRIPT_DELIMITERS", DefaultLanguageHighlighterColors.SEMICOLON)

    private val keys = mutableMapOf<IElementType, Array<TextAttributesKey>>()

    private fun put(token: IElementType, vararg keysArr: TextAttributesKey) {
      @Suppress("UNCHECKED_CAST")
      keys[token] = keysArr as Array<TextAttributesKey>
    }

    init {
      listOf(INTERPOLATION_START, INTERPOLATION_END).forEach { token ->
        put(token, HTML_CODE, MPX_INTERPOLATION_DELIMITER)
      }

      JSKeywordSets.AS_RESERVED_WORDS.types.forEach { token ->
        put(token, HTML_CODE, JSHighlighter.JS_KEYWORD)
      }

      listOf(
        pair(JSTokenTypes.STRING_LITERAL_PART, JSHighlighter.JS_STRING)
      ).forEach { p -> put(p.first, HTML_CODE, p.second) }
    }
  }

}
