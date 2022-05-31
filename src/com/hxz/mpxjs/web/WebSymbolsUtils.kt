// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.web

import com.intellij.javascript.web.codeInsight.html.elements.WebSymbolElementDescriptor
import com.hxz.mpxjs.model.MpxModelDirectiveProperties
import com.hxz.mpxjs.model.MpxModelDirectiveProperties.Companion.DEFAULT_EVENT
import com.hxz.mpxjs.model.MpxModelDirectiveProperties.Companion.DEFAULT_PROP
import com.hxz.mpxjs.web.MpxWebSymbolsAdditionalContextProvider.Companion.KIND_MPX_MODEL
import com.hxz.mpxjs.web.MpxWebSymbolsAdditionalContextProvider.Companion.PROP_MPX_MODEL_EVENT
import com.hxz.mpxjs.web.MpxWebSymbolsAdditionalContextProvider.Companion.PROP_MPX_MODEL_PROP

fun WebSymbolElementDescriptor.getModel(): MpxModelDirectiveProperties =
  runNameMatchQuery(listOf(KIND_MPX_MODEL)).firstOrNull()
    ?.let {
      MpxModelDirectiveProperties(prop = it.properties[PROP_MPX_MODEL_PROP] as? String ?: DEFAULT_PROP,
                                  event = it.properties[PROP_MPX_MODEL_EVENT] as? String ?: DEFAULT_EVENT)
    }
  ?: MpxModelDirectiveProperties()