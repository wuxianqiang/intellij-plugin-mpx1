// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.javascript.web.codeInsight.html.WebSymbolsXmlExtension
import com.intellij.javascript.web.codeInsight.html.elements.WebSymbolElementDescriptor
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.html.MpxLanguage
import com.hxz.mpxjs.model.MpxComponent
import com.hxz.mpxjs.model.MpxModelDirectiveProperties
import com.hxz.mpxjs.model.MpxModelManager
import com.hxz.mpxjs.web.getModel

class MpxXmlExtension : WebSymbolsXmlExtension() {
  override fun isAvailable(file: PsiFile?): Boolean =
    file?.let {
      it.language is MpxLanguage
      // Support extension in plain HTML with Mpx.js lib, PHP, Twig and others
      || (HTMLLanguage.INSTANCE in it.viewProvider.languages && isMpxContext(it))
    } == true

  override fun isRequiredAttributeImplicitlyPresent(tag: XmlTag?, attrName: String?): Boolean {
    if (attrName == null) return false

    val toAssetName = toAsset(attrName)
    val fromAssetName = fromAsset(attrName)

    return tag?.attributes?.find { attr ->
      if (attr.name == "v-bind") {
        return@find findExpressionInAttributeValue(attr, JSExpression::class.java)
          ?.let { JSResolveUtil.getElementJSType(it) }
          ?.asRecordType()
          ?.findPropertySignature(toAssetName) != null
      }
      val info = MpxAttributeNameParser.parse(attr.name, tag)
      var name: String? = null
      if (info is MpxAttributeNameParser.MpxDirectiveInfo) {
        if (info.directiveKind == MpxAttributeNameParser.MpxDirectiveKind.MODEL) {
          name = (tag.descriptor as? WebSymbolElementDescriptor)?.getModel()?.prop
                 ?: MpxModelDirectiveProperties.DEFAULT_PROP
        }
        else if (info.directiveKind === MpxAttributeNameParser.MpxDirectiveKind.BIND
                 && info.arguments != null) {
          name = info.arguments
        }
      }
      return@find fromAsset(name ?: info.name) == fromAssetName
    } != null
  }

  override fun isSelfClosingTagAllowed(tag: XmlTag): Boolean =
    isMpxComponentTemplateContext(tag)
    || super.isSelfClosingTagAllowed(tag)

  private fun isMpxComponentTemplateContext(tag: XmlTag) =
    tag.containingFile.let {
      it.virtualFile.fileType == MpxFileType.INSTANCE
      || MpxModelManager.findEnclosingContainer(it) is MpxComponent
    }

}
