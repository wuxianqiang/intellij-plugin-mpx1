// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html

import com.intellij.javascript.web.lang.html.WebFrameworkHtmlFileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.MpxjsIcons
import javax.swing.Icon

class MpxFileType private constructor(): WebFrameworkHtmlFileType(MpxLanguage.INSTANCE, "Mpx.js", "mpx") {
  companion object {
    @JvmField
    val INSTANCE: MpxFileType = MpxFileType()
  }
}
