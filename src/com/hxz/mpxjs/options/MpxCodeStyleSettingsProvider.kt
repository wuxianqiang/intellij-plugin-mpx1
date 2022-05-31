// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.options

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.*
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.lang.html.MpxLanguage
import com.hxz.mpxjs.lang.html.psi.formatter.MpxCodeStyleSettings

class MpxCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

  override fun getLanguage(): Language = MpxLanguage.INSTANCE

  override fun getCodeSample(settingsType: SettingsType): String? = """
      <template>
        <div id="app">
              <img      alt="Mpx logo"        
     src="./assets/logo.png">
        <HelloWorld  
     msg =  "Welcome to Your Mpx.js App"/></div>
     <span>{{descr    }}</span>
     <span>{{ (function (){ alert("Mpx is great!")   } return "Really great!")() }}</span>
    </template>
    
     <script>
        import HelloWorld  from './components/HelloWorld.mpx'
    
        export  default  {
      name:    'App'  ,
         components:     {
        HelloWorld}
      }
    </script>
    
      <style>
           #app      {
      font-family: Avenir, Helvetica, Arial, sans-serif;
       text-align: center;   color    : #2c3e50;}
    </style>
  """.trimIndent()

  override fun createFileFromText(project: Project, text: String): PsiFile? =
    PsiFileFactory.getInstance(project).createFileFromText(
      "a.{{.}}.#@injected@#.html", MpxLanguage.INSTANCE, text, false, true)

  override fun getIndentOptionsEditor(): IndentOptionsEditor? {
    return MpxIndentOptionsEditor()
  }

  override fun createConfigurable(baseSettings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
    return object : CodeStyleAbstractConfigurable(baseSettings, modelSettings, configurableDisplayName) {
      override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
        return MpxCodeStyleMainPanel(currentSettings, settings)
      }

      override fun getHelpTopic(): String? {
        return "reference.settingsdialog.IDE.mpxcodestyle"
      }
    }
  }

  override fun customizeDefaults(commonSettings: CommonCodeStyleSettings, indentOptions: CommonCodeStyleSettings.IndentOptions) {
    indentOptions.TAB_SIZE = 2
    indentOptions.INDENT_SIZE = 2
    indentOptions.CONTINUATION_INDENT_SIZE = 4
  }

  override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings? {
    return MpxCodeStyleSettings(settings)
  }

  override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
    when (settingsType) {
      SettingsType.SPACING_SETTINGS -> {
        consumer.showCustomOption(MpxCodeStyleSettings::class.java, "SPACES_WITHIN_INTERPOLATION_EXPRESSIONS",
                                  MpxBundle.message("mpx.formatting.spacing.within.interpolations"),
                                  MpxBundle.message("mpx.formatting.spacing.within.group"))
      }
      SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {

        consumer.showCustomOption(MpxCodeStyleSettings::class.java,
                                  "INTERPOLATION_WRAP",
                                  MpxBundle.message("mpx.formatting.wrapping.interpolations"),
                                  null,
                                  CodeStyleSettingsCustomizableOptions.getInstance().WRAP_OPTIONS,
                                  CodeStyleSettingsCustomizable.WRAP_VALUES)
        consumer.showCustomOption(MpxCodeStyleSettings::class.java,
                                  "INTERPOLATION_NEW_LINE_AFTER_START_DELIMITER",
                                  MpxBundle.message("mpx.formatting.wrapping.new-line-after-start-delimiter"),
                                  MpxBundle.message("mpx.formatting.wrapping.interpolations"))
        consumer.showCustomOption(MpxCodeStyleSettings::class.java,
                                  "INTERPOLATION_NEW_LINE_BEFORE_END_DELIMITER",
                                  MpxBundle.message("mpx.formatting.wrapping.new-line-before-end-delimiter"),
                                  MpxBundle.message("mpx.formatting.wrapping.interpolations"))

      }
      else -> {
      }
    }
  }
}