// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.run

import com.intellij.javascript.debugger.JavaScriptDebugAwareBase
import com.hxz.mpxjs.lang.html.MpxFileType

internal class MpxDebugAware : JavaScriptDebugAwareBase(MpxFileType.INSTANCE)
