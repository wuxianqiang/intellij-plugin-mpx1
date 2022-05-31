// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.web

import com.intellij.javascript.web.WebFramework
import com.intellij.javascript.web.lang.html.WebFrameworkHtmlFileType
import com.intellij.javascript.web.symbols.SymbolKind
import com.intellij.javascript.web.symbols.WebSymbol
import com.intellij.javascript.web.symbols.WebSymbolsContainer
import com.intellij.lang.javascript.JSStringUtil
import com.hxz.mpxjs.MpxjsIcons
import com.hxz.mpxjs.codeInsight.fromAsset
import com.hxz.mpxjs.lang.html.MpxFileType
import javax.swing.Icon

class MpxFramework : WebFramework() {

  override val displayName: String = "Mpx"
  override val icon: Icon = MpxjsIcons.Mpx
  override val standaloneFileType: WebFrameworkHtmlFileType = MpxFileType.INSTANCE
  override val htmlFileType: WebFrameworkHtmlFileType = MpxFileType.INSTANCE

  override fun getCanonicalNames(namespace: WebSymbolsContainer.Namespace,
                                 kind: SymbolKind,
                                 name: String,
                                 forQuery: Boolean): List<String> =
    if (namespace == WebSymbolsContainer.Namespace.HTML) {
      if (forQuery) {
        when (kind) {
          WebSymbol.KIND_HTML_VUE_COMPONENTS ->
            listOf(name, fromAsset(name, true))
          WebSymbol.KIND_HTML_VUE_COMPONENT_PROPS ->
            listOf(fromAsset(name))
          else -> emptyList()
        }
      }
      else {
        when (kind) {
          WebSymbol.KIND_HTML_VUE_COMPONENTS ->
            if (name.contains('-'))
              listOf(name)
            else
              listOf(fromAsset(name, true))
          WebSymbol.KIND_HTML_VUE_COMPONENT_PROPS ->
            listOf(fromAsset(name))
          else -> emptyList()
        }
      }

    }
    else emptyList()

  override fun getNameVariants(namespace: WebSymbolsContainer.Namespace, kind: SymbolKind, name: String): List<String> =
    if (namespace == WebSymbolsContainer.Namespace.HTML) {
      when (kind) {
        WebSymbol.KIND_HTML_VUE_COMPONENTS ->  if (name.contains('-'))
          listOf(name)
        else
          listOf(name, fromAsset(name))
        WebSymbol.KIND_HTML_VUE_COMPONENT_PROPS -> listOf(fromAsset(name))
        else -> emptyList()
      }
    }
    else emptyList()

  companion object {
    val instance get() = get("mpx")
  }
}
