// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.documentation

interface MpxDocumentedItem {
  val documentation: MpxItemDocumentation
    get() {
      return object : MpxItemDocumentation {
        override val defaultName: String? = MpxItemDocumentation.nameOf(this@MpxDocumentedItem)
        override val type: String = MpxItemDocumentation.typeOf(this@MpxDocumentedItem)
        override val library: String? = null
        override val description: String? = null
        override val docUrl: String? = null
      }
    }
}
