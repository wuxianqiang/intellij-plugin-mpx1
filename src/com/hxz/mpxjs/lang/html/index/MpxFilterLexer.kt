// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.index

import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.javascript.JSExtendedLanguagesTokenSetProvider
import com.intellij.lang.javascript.JSKeywordSets
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.JSTokenTypes.XML_NAME
import com.intellij.lang.javascript.refactoring.ECMAL4NamesValidator
import com.intellij.lang.xml.XMLLanguage
import com.intellij.lexer.Lexer
import com.intellij.psi.impl.cache.CacheUtil
import com.intellij.psi.impl.cache.impl.BaseFilterLexer
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer
import com.intellij.psi.impl.cache.impl.idCache.XmlFilterLexer
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.tree.TokenSet.create
import com.intellij.psi.tree.TokenSet.orSet
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.containers.ContainerUtil
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.lang.expr.MpxJSLanguage
import com.hxz.mpxjs.lang.html.MpxLanguage
import kotlin.experimental.or

class MpxFilterLexer(occurrenceConsumer: OccurrenceConsumer, originalLexer: Lexer) : BaseFilterLexer(originalLexer,
                                                                                                     occurrenceConsumer) {

  override fun advance() {
    val tokenType = myDelegate.tokenType
    if (!SKIP_WORDS.contains(tokenType)) {
      if (IDENTIFIERS.contains(tokenType)) {
        addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt())
      }
      else if (tokenType === XML_NAME) {
        val info = MpxAttributeNameParser.parse(myDelegate.tokenText)
        if (info.kind !== MpxAttributeNameParser.MpxAttributeKind.PLAIN) {
          if (info is MpxAttributeNameParser.MpxDirectiveInfo) {
            var nameIndex = tokenText.indexOf(info.name)
            if (nameIndex >= 0) {
              addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt(), nameIndex, info.name.length)
              nameIndex = info.name.length
            }
            if (info.arguments != null && ECMAL4NamesValidator.isIdentifier(info.arguments)) {
              addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt(), tokenText.indexOf(info.arguments, nameIndex), info.arguments.length)
            }
          }
          else {
            addOccurrenceInToken(UsageSearchContext.IN_CODE.toInt(), tokenText.indexOf(info.name), info.name.length)
          }
        }
        else {
          scanWordsInToken((UsageSearchContext.IN_PLAIN_TEXT or UsageSearchContext.IN_FOREIGN_LANGUAGES).toInt(),
                           tokenType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN,
                           false)
        }
      }
      else if (tokenType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN ||
               tokenType === XmlTokenType.XML_NAME ||
               tokenType === XmlTokenType.XML_TAG_NAME ||
               tokenType === XmlTokenType.XML_DATA_CHARACTERS) {
        scanWordsInToken((UsageSearchContext.IN_PLAIN_TEXT or UsageSearchContext.IN_FOREIGN_LANGUAGES).toInt(),
                         tokenType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN,
                         false)
      }
      else if (COMMENTS.contains(tokenType)) {
        scanWordsInToken(UsageSearchContext.IN_COMMENTS.toInt(), false, false)
        advanceTodoItemCountsInToken()
      }
      else if (LITERALS.contains(tokenType)) {
        scanWordsInToken(UsageSearchContext.IN_STRINGS.toInt(), false, false)
      }
      else if (tokenType != null && !SUPPORTED_LANGUAGES.contains(tokenType.language)) {
        val inComments = CacheUtil.isInComments(tokenType)
        scanWordsInToken(
          (if (inComments) UsageSearchContext.IN_COMMENTS else UsageSearchContext.IN_PLAIN_TEXT or UsageSearchContext.IN_FOREIGN_LANGUAGES).toInt(),
          true,
          false)

        if (inComments) advanceTodoItemCountsInToken()
      }
      else {
        scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT.toInt(), false, false)
      }
    }
    myDelegate.advance()
  }

  companion object {

    private val SUPPORTED_LANGUAGES = ContainerUtil.newHashSet(
      XMLLanguage.INSTANCE,
      HTMLLanguage.INSTANCE,
      MpxLanguage.INSTANCE,
      MpxJSLanguage.INSTANCE,
      Language.ANY
    )

    private val IDENTIFIERS = orSet(
      JSKeywordSets.IDENTIFIER_NAMES
    )

    private val COMMENTS = orSet(
      JSTokenTypes.COMMENTS,
      create(XmlTokenType.XML_COMMENT_CHARACTERS)
    )

    private val LITERALS = orSet(
      JSTokenTypes.LITERALS
    )

    private val SKIP_WORDS = orSet(
      JSExtendedLanguagesTokenSetProvider.SKIP_WORDS_SCAN_SET,
      XmlFilterLexer.NO_WORDS_TOKEN_SET,
      create(
        XmlTokenType.XML_COMMA
        // MpxTokenTypes.ESCAPE_SEQUENCE,
      )
    )
  }
}
