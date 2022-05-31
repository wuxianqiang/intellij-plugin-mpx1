// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.typescript.service

import com.intellij.lang.javascript.DialectDetector
import com.intellij.lang.typescript.compiler.languageService.protocol.TypeScriptLanguageServiceCache
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.hxz.mpxjs.index.findModule
import com.hxz.mpxjs.lang.html.MpxFileType

class MpxTypeScriptServiceCache(project: Project) : TypeScriptLanguageServiceCache(project) {
  
  override fun addFileIfInvalid(file: VirtualFile,
                               filesToClose: Set<VirtualFile>,
                               toCloseByChangedType: MutableSet<VirtualFile>) {
    if (isInvalidMpxTypeScriptFile(file) && !filesToClose.contains(file)) toCloseByChangedType.add(file)
  }

  private fun isInvalidMpxTypeScriptFile(file: VirtualFile): Boolean {
    if (file.isValid) {
      val fileType = file.fileType
      if (fileType == MpxFileType.INSTANCE) {
        val findFile = PsiManager.getInstance(myProject).findFile(file)
        if (findFile == null) return false

        val module = findModule(findFile)
        return module == null || !DialectDetector.isTypeScript(module)
      }
    }
    return false
  }
}
