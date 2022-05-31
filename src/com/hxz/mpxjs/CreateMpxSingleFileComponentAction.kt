// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.jetbrains.annotations.Nls
import com.hxz.mpxjs.context.hasMpxFiles
import com.hxz.mpxjs.context.isMpxContext

class CreateMpxSingleFileComponentAction : CreateFileFromTemplateAction(MpxBundle.message("mpx.create.single.file.component.action.text"),
                                                                        MpxBundle.message(
                                                                          "mpx.create.single.file.component.action.description"),
                                                                        MpxjsIcons.Mpx), DumbAware {
  companion object {
    const val MPX_TEMPLATE_NAME: String = "Mpx Single File Component"

    @Nls
    private val name = MpxBundle.message("mpx.create.single.file.component.action.text")
  }

  override fun isAvailable(dataContext: DataContext): Boolean {
    return super.isAvailable(dataContext)
           && (PROJECT.getData(dataContext)?.let { hasMpxFiles(it) } == true
               || (PSI_ELEMENT.getData(dataContext) ?: PSI_FILE.getData(dataContext))?.let { isMpxContext(it) } == true)
  }

  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    builder
      .setTitle(MpxBundle.message("mpx.create.single.file.component.action.dialog.title", name))
      .addKind(name, MpxjsIcons.Mpx, MPX_TEMPLATE_NAME)
  }

  override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
    MpxBundle.message("mpx.create.single.file.component.action.name", name, newName)

}
