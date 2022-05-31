// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.arrangement

import com.intellij.application.options.codeStyle.CodeStyleSchemesModel
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.xml.arrangement.HtmlRearranger
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxArrangementSettingsMigration : StartupActivity, StartupActivity.DumbAware {


  override fun runActivity(project: Project) {
    val propertiesComponent = PropertiesComponent.getInstance(project)
    if (!propertiesComponent.isTrueValue(MPX_REARRANGER_SETTINGS_MIGRATION)) {
      ApplicationManager.getApplication().invokeLaterOnWriteThread {
        if (project.isDisposed || propertiesComponent.isTrueValue(MPX_REARRANGER_SETTINGS_MIGRATION)) return@invokeLaterOnWriteThread
        propertiesComponent.setValue(MPX_REARRANGER_SETTINGS_MIGRATION, true)
        val codeStyleSchemesModel = CodeStyleSchemesModel(project)
        var changed = false
        codeStyleSchemesModel.schemes.asSequence()
          .map { it.codeStyleSettings }
          .forEach { codeStyleSettings ->
            codeStyleSettings
              .getCommonSettings(HTMLLanguage.INSTANCE)
              .takeIf { it != HtmlRearranger().defaultSettings }
              ?.arrangementSettings
              ?.let {
                val mpxSettings = codeStyleSettings
                  .getCommonSettings(MpxLanguage.INSTANCE)
                if (mpxSettings.arrangementSettings == null) {
                  mpxSettings.setArrangementSettings(it)
                  changed = true
                }
              }
          }
        if (changed) {
          codeStyleSchemesModel.apply()
        }
      }
    }
  }

  companion object {
    const val MPX_REARRANGER_SETTINGS_MIGRATION = "mpx.rearranger.settings.migration"
  }

}