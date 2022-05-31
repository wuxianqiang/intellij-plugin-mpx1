// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.formatter.xml.XmlBlock
import com.intellij.psi.formatter.xml.XmlTagBlock
import com.hxz.mpxjs.lang.html.MpxLanguage
import java.util.*

class MpxTagBlock(node: ASTNode,
                  wrap: Wrap?,
                  alignment: Alignment?,
                  policy: MpxRootFormattingPolicy,
                  indent: Indent?,
                  preserveSpace: Boolean)
  : XmlTagBlock(node, wrap, alignment, policy, indent, preserveSpace) {

  override fun createTagBlock(child: ASTNode, indent: Indent?, wrap: Wrap?, alignment: Alignment?): XmlTagBlock {
    return MpxHtmlTagBlock(child, wrap, alignment, (myXmlFormattingPolicy as MpxRootFormattingPolicy).htmlPolicy,
                           indent ?: Indent.getNoneIndent(), isPreserveSpace)
  }

  override fun createSimpleChild(child: ASTNode, indent: Indent?, wrap: Wrap?, alignment: Alignment?, range: TextRange?): XmlBlock {
    return MpxBlock(child, wrap, alignment, myXmlFormattingPolicy as MpxRootFormattingPolicy, indent, range, isPreserveSpace)
  }

  override fun createSyntheticBlock(localResult: ArrayList<Block>, childrenIndent: Indent?): Block {
    return MpxSyntheticBlock(localResult, this, Indent.getNoneIndent(),
                             (myXmlFormattingPolicy as MpxRootFormattingPolicy).htmlPolicy,
                             childrenIndent, null)
  }

  override fun useMyFormatter(myLanguage: Language, childLanguage: Language, childPsi: PsiElement): Boolean {
    return childLanguage === MpxLanguage.INSTANCE || super.useMyFormatter(myLanguage, childLanguage, childPsi)
  }
}