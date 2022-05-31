// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.arrangement

import com.intellij.psi.codeStyle.arrangement.ArrangementSettingsSerializer
import com.intellij.psi.codeStyle.arrangement.DefaultArrangementSettingsSerializer
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens
import com.intellij.xml.arrangement.HtmlRearranger

class MpxRearranger : HtmlRearranger() {


  override fun getDefaultSettings(): StdArrangementSettings? {
    return DEFAULT_SETTINGS
  }

  override fun getSerializer(): ArrangementSettingsSerializer {
    return SETTINGS_SERIALIZER
  }

  companion object {

    val DEFAULT_SETTINGS: StdArrangementSettings = StdArrangementSettings.createByMatchRules(
      emptyList(), MpxAttributeKind.values().map { it.createRule() })

    private val SETTINGS_SERIALIZER = DefaultArrangementSettingsSerializer(DEFAULT_SETTINGS)
  }

  enum class MpxAttributeKind(val pattern: String) {
    //DEFINITION("(wx:)?is"),
    LIST_RENDERING("wx:for"),
    CONDITIONALS("wx:(if|elif|else|show)"),
    //RENDER_MODIFIERS("wx:(pre|once)"),
    GLOBAL("style"),
    UNIQUE("(wx:)?(ref|key)"),
    TWO_WAY_BINDING("wx:model"),
    //OTHER_DIRECTIVES("wx:(?!on:|bind:|(html|text)$).+"),
    //OTHER_ATTR("(?!wx:on:|@|wx:html$|wx:text$).+"),
    EVENTS("(bind:|catch:|bind|catch)\\w+");
    //CONTENT("wx:html|wx:text");

    fun createRule(): StdArrangementMatchRule {
      return StdArrangementMatchRule(StdArrangementEntryMatcher(ArrangementCompositeMatchCondition(
        listOf(
          ArrangementAtomMatchCondition(StdArrangementTokens.EntryType.XML_ATTRIBUTE),
          ArrangementAtomMatchCondition(StdArrangementTokens.Regexp.NAME, pattern))
      )), StdArrangementTokens.Order.BY_NAME)
    }

  }

}
