// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.index

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubIndexKey

class MpxIdIndex : MpxIndexBase<PsiElement>(KEY, JS_KEY) {
  companion object {
    val KEY: StubIndexKey<String, PsiElement> =
      StubIndexKey.createIndexKey<String, PsiElement>("mpx.id.index")

    val JS_KEY: String = createJSKey(KEY)
  }
}

