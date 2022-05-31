// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.attributes

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.css.CSSLanguage
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.ContainerUtil
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxAttributeKind.*
import com.hxz.mpxjs.model.getAvailableSlots
import java.util.*

// TODO move to web-types
class MpxAttributeValueCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val MPX_SCRIPT_LANGUAGE = ContainerUtil.immutableSet("js", "ts")
  private val MPX_STYLE_LANGUAGE = mpxStyleLanguages()
  private val MPX_TEMPLATE_LANGUAGE = ContainerUtil.immutableSet("html", "pug")

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val xmlTag = PsiTreeUtil.getParentOfType(parameters.position, XmlTag::class.java, false)
    val xmlAttribute = PsiTreeUtil.getParentOfType(parameters.position, XmlAttribute::class.java,
                                                   false)
    if (xmlTag == null || xmlAttribute == null) return

    for (completion in listOfCompletions(xmlTag, xmlAttribute)) {
      result.addElement(LookupElementBuilder.create(completion))
    }
  }

  private fun listOfCompletions(xmlTag: XmlTag, xmlAttribute: XmlAttribute): Set<String> =
    when (MpxAttributeNameParser.parse(xmlAttribute.name, xmlTag).kind) {
      SCRIPT_LANG -> MPX_SCRIPT_LANGUAGE
      STYLE_LANG -> MPX_STYLE_LANGUAGE
      TEMPLATE_LANG -> MPX_TEMPLATE_LANGUAGE
      SLOT -> getAvailableSlots(xmlAttribute, false).map { it.name }.toSet()
      else -> emptySet()
    }

  private fun mpxStyleLanguages(): Set<String> {
    val result = mutableListOf<String>()
    result.add("css")
    CSSLanguage.INSTANCE.dialects.forEach {
      if (it.displayName != "JQuery-CSS") {
        result.add(it.displayName.toLowerCase(Locale.US))
      }
    }
    return result.toSet()
  }
}

