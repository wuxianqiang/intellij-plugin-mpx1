// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.highlighting

import com.intellij.lang.javascript.highlighting.TypeScriptHighlighter
import com.hxz.mpxjs.lang.expr.MpxJSLanguage


class MpxJSSyntaxHighlighter : TypeScriptHighlighter(MpxJSLanguage.INSTANCE.optionHolder, false)
