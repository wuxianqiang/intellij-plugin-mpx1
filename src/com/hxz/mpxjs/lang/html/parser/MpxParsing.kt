// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.html.HtmlParsing
import com.intellij.psi.tree.IElementType
import com.intellij.psi.xml.XmlElementType
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.HtmlUtil.*
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxAttributeKind.*
import com.hxz.mpxjs.lang.expr.parser.MpxJSEmbeddedExprTokenType
import com.hxz.mpxjs.lang.html.lexer.MpxTokenTypes.Companion.INTERPOLATION_END
import com.hxz.mpxjs.lang.html.lexer.MpxTokenTypes.Companion.INTERPOLATION_START
import com.hxz.mpxjs.model.SLOT_TAG_NAME
import java.util.*

class MpxParsing(builder: PsiBuilder) : HtmlParsing(builder) {

  override fun isSingleTag(tagName: String, originalTagName: String): Boolean {
    // There are heavily-used Mpx components called like 'Col' or 'Input'. Unlike HTML tags <col> and <input> Mpx components do have closing tags.
    // The following 'if' is a little bit hacky but it's rather tricky to solve the problem in a better way at parser level.
    if (tagName.length >= 3
        && tagName != originalTagName
        && !originalTagName.all { it.isUpperCase() }) {
      return false
    }
    return super.isSingleTag(tagName, originalTagName)
  }

  override fun hasCustomTagContent(): Boolean {
    return token() === INTERPOLATION_START
  }

  override fun hasCustomTopLevelContent(): Boolean {
    return token() === INTERPOLATION_START
  }

  override fun parseCustomTagContent(xmlText: PsiBuilder.Marker?): PsiBuilder.Marker? {
    var result = xmlText
    val tt = token()
    if (tt === INTERPOLATION_START) {
      result = terminateText(result)
      val interpolation = mark()
      advance()
      if (token() is MpxJSEmbeddedExprTokenType) {
        advance()
      }
      if (token() === INTERPOLATION_END) {
        advance()
        interpolation.drop()
      }
      else {
        interpolation.error(MpxBundle.message("mpx.parser.message.unterminated.interpolation"))
      }
    }
    return result
  }

  override fun parseCustomTopLevelContent(error: PsiBuilder.Marker?): PsiBuilder.Marker? {
    val result = flushError(error)
    terminateText(parseCustomTagContent(null))
    return result
  }

  override fun parseAttribute() {
    assert(token() === XmlTokenType.XML_NAME)
    val attr = mark()
    val attributeInfo = MpxAttributeNameParser.parse(builder.tokenText!!, peekTagName(), tagLevel() == 1)
    advance()
    if (token() === XmlTokenType.XML_EQ) {
      advance()
      parseAttributeValue()
    }
    if (peekTagName().toLowerCase(Locale.US) == SLOT_TAG_NAME) {
      attr.done(MpxStubElementTypes.MPX_STUBBED_ATTRIBUTE)
    }
    else
      when (attributeInfo.kind) {
        TEMPLATE_SRC, SCRIPT_SRC, STYLE_SRC -> attr.done(MpxStubElementTypes.SRC_ATTRIBUTE)
        SCRIPT_ID -> attr.done(MpxStubElementTypes.SCRIPT_ID_ATTRIBUTE)
        SCRIPT_SETUP, STYLE_MODULE -> attr.done(MpxStubElementTypes.MPX_STUBBED_ATTRIBUTE)
        REF -> attr.done(MpxStubElementTypes.REF_ATTRIBUTE)
        else -> attr.done(XmlElementType.XML_ATTRIBUTE)
      }
  }

  override fun getHtmlTagElementType(): IElementType {
    val tagName = peekTagName().toLowerCase(Locale.US)
    if (tagName in STUBBED_TAGS
        || (tagLevel() == 1 && tagName in TOP_LEVEL_TAGS)) {
      return MpxStubElementTypes.STUBBED_TAG
    }
    return super.getHtmlTagElementType()
  }

  companion object {
    val STUBBED_TAGS: List<String> = listOf(SCRIPT_TAG_NAME, SLOT_TAG_NAME)
    val TOP_LEVEL_TAGS: List<String> = listOf(TEMPLATE_TAG_NAME, STYLE_TAG_NAME)
  }
}
