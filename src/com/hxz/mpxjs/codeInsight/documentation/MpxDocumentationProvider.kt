// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.documentation

import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.codeInsight.refs.MpxJSReferenceExpressionResolver
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterReferenceExpression

class MpxDocumentationProvider : DocumentationProvider {

  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    return null
  }

  override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String>? {
    return (element as? PsiWrappedMpxDocumentedItem)?.let {
      it.item.docUrl?.let { url -> mutableListOf(url) } ?: mutableListOf()
    }
  }

  override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?, targetOffset: Int): PsiElement? {
    return getMpxDocumentedItem(contextElement, targetOffset)
      ?.let { PsiWrappedMpxDocumentedItem(it.first, it.second) }
  }

  override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
    return (element as? PsiWrappedMpxDocumentedItem)
      ?.let { generateDoc(it.item) }
  }

  override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, `object`: Any?, element: PsiElement?): PsiElement? {
    val documentationItem = `object`?.castSafelyTo<Pair<*, *>>()
      ?.first
      ?.castSafelyTo<MpxItemDocumentation>()
    if (documentationItem != null && element != null) {
      return PsiWrappedMpxDocumentedItem(documentationItem, element)
    }
    return null
  }

  private fun getMpxDocumentedItem(originalElement: PsiElement?, offset: Int): Pair<MpxItemDocumentation, PsiElement>? {
    val docSource = when (originalElement?.node?.elementType) {
      JSTokenTypes.IDENTIFIER -> originalElement?.parent
      else -> originalElement
    }
    return when (docSource) {
      is MpxJSFilterReferenceExpression -> MpxJSReferenceExpressionResolver
        .resolveFiltersFromReferenceExpression(docSource)
        .getOrNull(0)
        ?.let { Pair(it.documentation, docSource) }
      else -> null
    }
  }

  private fun generateDoc(item: MpxItemDocumentation): String? {
    val result = StringBuilder().append(DEFINITION_START)
    val name = item.defaultName ?: ""
    if (name.isBlank()) {
      result.append(MpxBundle.message("mpx.documentation.mpx") + " ").append(item.type)
    }
    else {
      result.append(name)
    }
    result.append(DEFINITION_END)
    item.description?.let { result.append(CONTENT_START).append(it).append(CONTENT_END) }

    val details = LinkedHashMap(item.customSections)
    item.library?.let { details[MpxBundle.message("mpx.documentation.section.library")] = "<p>${if (it == "mpx") "Mpx" else it}" }

    if (details.isNotEmpty()) {
      result.append(SECTIONS_START)
      details.entries.forEach { (name, value) ->
        result.append(SECTION_HEADER_START).append(name)
          .append(SECTION_SEPARATOR).append(value).append(SECTION_END)
      }
      result.append(SECTIONS_END)
    }
    return result.toString()
  }

  private class PsiWrappedMpxDocumentedItem(val item: MpxItemDocumentation,
                                            private val source: PsiElement) : FakePsiElement(), PsiNamedElement {
    override fun getParent(): PsiElement = source
    override fun getName(): String = item.defaultName ?: (MpxBundle.message("mpx.documentation.mpx") + " " + item.type)
  }

}
