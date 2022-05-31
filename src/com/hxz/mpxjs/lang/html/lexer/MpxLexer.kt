// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.lexer

import com.intellij.lang.javascript.dialects.JSLanguageLevel
import com.intellij.openapi.project.Project

interface MpxLexer {

  val languageLevel: JSLanguageLevel

  val project: Project?

  val interpolationConfig: Pair<String, String>?
}