// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.formatter.xml.XmlBlock
import com.intellij.psi.formatter.xml.XmlFormattingPolicy
import com.intellij.psi.formatter.xml.XmlTagBlock
import com.hxz.mpxjs.lang.html.MpxLanguage
import com.hxz.mpxjs.lang.html.lexer.MpxTokenTypes.Companion.INTERPOLATION_START
import java.util.*

class MpxHtmlTagBlock(node: ASTNode,
                      wrap: Wrap?,
                      alignment: Alignment?,
                      policy: XmlFormattingPolicy,
                      indent: Indent?,
                      preserveSpace: Boolean)
  : XmlTagBlock(node, wrap, alignment, policy, indent, preserveSpace), BlockEx {

  override fun getLanguage(): Language? {
    return HTMLLanguage.INSTANCE
  }

  override fun createTagBlock(child: ASTNode, indent: Indent?, wrap: Wrap?, alignment: Alignment?): XmlTagBlock {
    return MpxHtmlTagBlock(child, wrap, alignment, myXmlFormattingPolicy, indent ?: Indent.getNoneIndent(),
                           isPreserveSpace)
  }

  override fun createSimpleChild(child: ASTNode, indent: Indent?, wrap: Wrap?, alignment: Alignment?, range: TextRange?): XmlBlock {
    return MpxHtmlBlock(child, wrap, alignment, myXmlFormattingPolicy, indent, range, isPreserveSpace)
  }

  override fun createSyntheticBlock(localResult: ArrayList<Block>, childrenIndent: Indent?): Block {
    return MpxSyntheticBlock(localResult, this, Indent.getNoneIndent(), myXmlFormattingPolicy, childrenIndent, HTMLLanguage.INSTANCE)
  }

  override fun useMyFormatter(myLanguage: Language, childLanguage: Language, childPsi: PsiElement): Boolean {
    return childLanguage === MpxLanguage.INSTANCE || super.useMyFormatter(myLanguage, childLanguage, childPsi)
  }

  override fun chooseWrap(child: ASTNode?, tagBeginWrap: Wrap?, attrWrap: Wrap?, textWrap: Wrap?): Wrap? =
    if (child?.elementType == INTERPOLATION_START) textWrap
    else super.chooseWrap(child, tagBeginWrap, attrWrap, textWrap)
}
