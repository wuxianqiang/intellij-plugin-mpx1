// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.index

import com.intellij.lang.javascript.psi.JSImplicitElementProvider
import com.intellij.psi.stubs.StubIndexKey

class MpxGlobalFiltersIndex : MpxIndexBase<JSImplicitElementProvider>(KEY, JS_KEY) {
  companion object {
    val KEY: StubIndexKey<String, JSImplicitElementProvider> =
      StubIndexKey.createIndexKey<String, JSImplicitElementProvider>("mpx.global.filters.index")
    val JS_KEY: String = createJSKey(KEY)
  }
}
