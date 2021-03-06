// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.html.HTMLParser

class MpxParser : HTMLParser() {
  override fun createHtmlParsing(builder: PsiBuilder): MpxParsing = MpxParsing(builder)
}
