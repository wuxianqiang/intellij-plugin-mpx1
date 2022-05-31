// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.attributes

import com.intellij.html.impl.providers.HtmlAttributeValueProvider
import com.intellij.javascript.web.codeInsight.css.refs.CssClassInJSLiteralOrIdentifierReferenceProvider.Companion.getClassesFromEmbeddedContent
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.SmartList
import com.intellij.xml.util.HtmlUtil
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.Companion.parse
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.lang.expr.psi.MpxJSEmbeddedExpression

class MpxCustomAttributeValueProvider : HtmlAttributeValueProvider() {

  override fun getCustomAttributeValues(tag: XmlTag, attributeName: String): String? {
    if (attributeName.equals(HtmlUtil.CLASS_ATTRIBUTE_NAME, ignoreCase = true)
        && isMpxContext(tag)) {
      val result = SmartList<String>()
      var classAttr: String? = null
      for (attribute in tag.attributes) {
        val attrName = attribute.name
        if (HtmlUtil.CLASS_ATTRIBUTE_NAME.equals(attrName, ignoreCase = true)) {
          classAttr = attribute.value
        }
        else {
          getCustomAttributeValues(tag, attrName)?.let { result.add(it) }
        }
      }
      if (!result.isEmpty()) {
        classAttr?.let { result.add(it) }
        return StringUtil.join(result, " ")
      }
      return null
    }
    else if (isVBindClassAttribute(parse(attributeName, tag))) {
      return getClassNames(tag.getAttribute(attributeName))
    }
    return null
  }

  companion object {

    fun isVBindClassAttribute(attribute: XmlAttribute?): Boolean =
      attribute
        ?.let { parse(it.name, it.parent) }
        .let { isVBindClassAttribute(it) }

    fun isVBindClassAttribute(info: MpxAttributeNameParser.MpxAttributeInfo?): Boolean =
      info is MpxAttributeNameParser.MpxDirectiveInfo
      && info.directiveKind == MpxAttributeNameParser.MpxDirectiveKind.BIND
      && info.arguments == HtmlUtil.CLASS_ATTRIBUTE_NAME

    private fun getClassNames(attribute: XmlAttribute?): String {
      return getClassesFromEmbeddedContent(PsiTreeUtil.findChildOfType(attribute, MpxJSEmbeddedExpression::class.java))
    }
  }
}
