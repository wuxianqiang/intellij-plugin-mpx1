// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.typescript.service

import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.lang.javascript.service.JSLanguageService
import com.intellij.lang.javascript.service.highlighting.JSLanguageServiceHighlightingPassFactory
import com.intellij.lang.typescript.compiler.TypeScriptServiceHighlightingPassFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.hxz.mpxjs.lang.html.MpxFileType

internal class MpxTypeScriptServiceHighlightingPassFactoryRegistrar : TextEditorHighlightingPassFactoryRegistrar {
  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    class MyFactory : JSLanguageServiceHighlightingPassFactory() {
      override fun getService(file: PsiFile): JSLanguageService? {
        val service = TypeScriptServiceHighlightingPassFactory.getService(file.project, file)
        return if (service is MpxTypeScriptService) service else null
      }

      override fun isAcceptablePsiFile(file: PsiFile): Boolean {
        return file.fileType == MpxFileType.INSTANCE
      }
    }

    val factory = MyFactory()
    JSLanguageServiceHighlightingPassFactory.registerHighlightingPassFactory(registrar, factory)
  }
}
