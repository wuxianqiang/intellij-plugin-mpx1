// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.parser

import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterLazyParseableNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.psi.tree.ILightLazyParseableElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import com.hxz.mpxjs.lang.html.MpxLanguage

object MpxElementTypes {

  val MPX_EMBEDDED_CONTENT: IElementType = EmbeddedMpxContentElementType()

  class EmbeddedMpxContentElementType : ILazyParseableElementType("MPX_EMBEDDED_CONTENT",
                                                                  MpxLanguage.INSTANCE), ILightLazyParseableElementType {

    override fun parseContents(chameleon: LighterLazyParseableNode): FlyweightCapableTreeStructure<LighterASTNode> {
      val file = chameleon.containingFile ?: error(chameleon)

      val builder = PsiBuilderFactory.getInstance().createBuilder(file.project, chameleon)
      MpxParser().parseWithoutBuildingTree(MpxFileElementType.INSTANCE, builder)
      return builder.lightTree
    }
  }
}
