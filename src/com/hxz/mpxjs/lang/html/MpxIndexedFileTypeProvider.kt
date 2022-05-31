// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html

import com.intellij.lang.javascript.index.IndexedFileTypeProvider
import com.intellij.openapi.fileTypes.FileType

class MpxIndexedFileTypeProvider : IndexedFileTypeProvider {
  override fun getFileTypesToIndex(): Array<FileType> = arrayOf(MpxFileType.INSTANCE)
}
