// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.documentation

import com.intellij.util.IncorrectOperationException
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.model.*

interface MpxItemDocumentation {
  /**
   * Default symbol name
   */
  val defaultName: String?

  /**
   * Symbol type
   */
  val type: String

  /**
   * Description of the entity with HTML markup
   */
  val description: String?

  /**
   * URL for external documentation
   */
  val docUrl: String?

  /**
   * Library of origin
   */
  val library: String?

  /**
   * Custom section to display in the documentation
   */
  val customSections: Map<String, String> get() = emptyMap()

  companion object {

    fun typeOf(item: MpxDocumentedItem): String =
      when (item) {
        is MpxFunctionComponent -> "mpx.documentation.type.functional.component"
        is MpxComponent -> "mpx.documentation.type.component"
        is MpxDirective -> "mpx.documentation.type.directive"
        is MpxFilter -> "mpx.documentation.type.filter"
        is MpxMethod -> "mpx.documentation.type.component.method"
        is MpxEmitCall -> "mpx.documentation.type.component.event"
        is MpxSlot -> "mpx.documentation.type.slot"
        is MpxInputProperty -> "mpx.documentation.type.component.property"
        is MpxComputedProperty -> "mpx.documentation.type.component.computed.property"
        is MpxDataProperty -> "mpx.documentation.type.component.data.property"
        is MpxDirectiveModifier -> "mpx.documentation.type.directive.modifier"
        is MpxDirectiveArgument -> "mpx.documentation.type.directive.argument"
        else -> throw IncorrectOperationException(item.javaClass.name)
      }.let { MpxBundle.message(it) }

    fun nameOf(item: MpxDocumentedItem): String? =
      when (item) {
        is MpxNamedEntity -> item.defaultName
        is MpxNamedSymbol -> item.name
        is MpxDirectiveArgument -> null
        else -> throw IncorrectOperationException(item.javaClass.name)
      }

    fun createSections(item: MpxDocumentedItem): Map<String, String> {
      val sections = LinkedHashMap<String, String>()
      when (item) {
        is MpxDirective -> {
          item.argument?.documentation?.description?.let { sections["mpx.documentation.section.argument"] = it }
        }
        is MpxDirectiveArgument -> {
          if (item.required) {
            sections["mpx.documentation.section.required"] = ""
          }
          item.pattern?.let { sections["mpx.documentation.section.pattern"] = it.toString() }
        }
        is MpxDirectiveModifier -> {
          item.pattern?.let { sections["mpx.documentation.section.pattern"] = it.toString() }
        }
        is MpxSlot -> {
          item.pattern?.let { sections["mpx.documentation.section.pattern"] = it.toString() }
        }
        is MpxInputProperty -> {
          if (item.required) {
            sections["mpx.documentation.section.required"] = ""
          }
          item.defaultValue
            ?.takeIf { it != "null" }
            ?.let { sections["mpx.documentation.section.default"] = it }
        }
      }
      return sections
        .map { (key, value) -> Pair(MpxBundle.message(key), value) }
        .toMap()
    }
  }
}
