// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.highlighting

import com.intellij.lang.javascript.dialects.JSLanguageLevel
import com.intellij.lang.javascript.settings.JSRootConfiguration
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.hxz.mpxjs.lang.html.parser.MpxFileElementType

class MpxSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
    return MpxFileHighlighter(
      project?.let { JSRootConfiguration.getInstance(it).languageLevel } ?: JSLanguageLevel.getLevelForJSX(),
      project,
      MpxFileElementType.readDelimiters(virtualFile?.name))
  }
}
