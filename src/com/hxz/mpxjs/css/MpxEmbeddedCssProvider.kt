// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.css

import com.intellij.lang.Language
import com.intellij.psi.css.EmbeddedCssProvider
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxEmbeddedCssProvider : EmbeddedCssProvider() {
  override fun enableEmbeddedCssFor(language: Language): Boolean = language is MpxLanguage
}
