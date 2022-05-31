// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.tags

import com.intellij.lang.ecmascript6.psi.JSExportAssignment
import com.intellij.lang.javascript.psi.JSNamedElement
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.impl.source.xml.XmlDescriptorUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlElementsGroup
import com.intellij.xml.XmlNSDescriptor
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeDescriptor
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeDescriptor.AttributePriority.LOW
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxAttributeKind.PLAIN
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxDirectiveKind.BIND
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxDirectiveKind.CUSTOM
import com.hxz.mpxjs.codeInsight.fromAsset
import com.hxz.mpxjs.model.*

class MpxElementDescriptor(private val tag: XmlTag, private val sources: Collection<MpxComponent> = emptyList()) : XmlElementDescriptor {

  override fun getDeclaration(): JSImplicitElement =
    sources.asSequence()
      .map { it.source }
      .map {
        if (it !is JSImplicitElement
            && (it is JSNamedElement || it is JSObjectLiteralExpression || it is JSExportAssignment))
          JSImplicitElementImpl.Builder((it as? JSNamedElement)?.name ?: "<anonymous>", it)
            .forbidAstAccess().toImplicitElement()
        else
          it
      }
      .filterIsInstance<JSImplicitElement>()
      .firstOrNull()
    ?: JSImplicitElementImpl(JSImplicitElementImpl.Builder(tag.name, tag).forbidAstAccess())

  override fun getName(context: PsiElement?): String = (context as? XmlTag)?.name ?: name
  override fun getName(): String = fromAsset(declaration.name)
  override fun init(element: PsiElement?) {}
  override fun getQualifiedName(): String = name
  override fun getDefaultName(): String = name

  override fun getElementsDescriptors(context: XmlTag): Array<XmlElementDescriptor> =
    XmlDescriptorUtil.getElementsDescriptors(context)

  override fun getElementDescriptor(childTag: XmlTag, contextTag: XmlTag): XmlElementDescriptor? =
    XmlDescriptorUtil.getElementDescriptor(childTag, contextTag)

  override fun getAttributesDescriptors(context: XmlTag?): Array<out XmlAttributeDescriptor> {
    val result = mutableListOf<XmlAttributeDescriptor>()
    val commonHtmlAttributes = HtmlNSDescriptorImpl.getCommonAttributeDescriptors(context)
    result.addAll(commonHtmlAttributes)
    result.addAll(getProps())
    return result.toTypedArray()
  }

  fun getSources(): List<MpxComponent> =
    sources.toList()

  fun getPsiSources(): List<PsiElement> =
    sources.mapNotNull { it.source }
      .ifEmpty {
        listOf(JSImplicitElementImpl(JSImplicitElementImpl.Builder(tag.name, tag).forbidAstAccess()))
      }

  fun getProps(): List<XmlAttributeDescriptor> =
    sources.flatMap {
      val result = mutableListOf<XmlAttributeDescriptor>()
      it.acceptPropertiesAndMethods(object : MpxModelVisitor() {
        override fun visitInputProperty(prop: MpxInputProperty, proximity: Proximity): Boolean {
          result.add(MpxAttributeDescriptor(tag, fromAsset(prop.name), prop))
          return true
        }
      })
      result
    }

  fun getSlots(): List<MpxSlot> =
    sources.flatMap { (it as? MpxContainer)?.slots ?: emptyList() }

  fun getEmitCalls(): List<MpxEmitCall> =
    sources.flatMap { (it as? MpxContainer)?.emits ?: emptyList() }

  fun getModel(): MpxModelDirectiveProperties =
    sources.asSequence()
      .filterIsInstance<MpxContainer>()
      .map { it.model }
      .firstOrNull()
    ?: MpxModelDirectiveProperties()

  override fun getAttributeDescriptor(attributeName: String?, context: XmlTag?): XmlAttributeDescriptor? {
    val info = MpxAttributeNameParser.parse(attributeName ?: return null,
                                            context ?: return null)

    if (info is MpxAttributeNameParser.MpxDirectiveInfo && info.arguments != null) {
      if (info.directiveKind === BIND) {
        return resolveToProp(context, info.arguments, attributeName)
               ?: MpxAttributeDescriptor(context, attributeName, acceptsNoValue = false, priority = LOW)
      }
      // TODO resolve component events
    }
    if (info.kind === PLAIN) {
      resolveToProp(context, info.name, attributeName)?.let { return it }
    }

    return HtmlNSDescriptorImpl.getCommonAttributeDescriptor(attributeName, context)
           // relax attributes check: https://mpxjs.org/v2/guide/components.html#Non-Prop-Attributes
           // mpx allows any non-declared as props attributes to be passed to a component
           ?: MpxAttributeDescriptor(context,
                                     attributeName,
                                     acceptsNoValue = !info.requiresValue
                                                      || info.kind === PLAIN
                                                      || (info is MpxAttributeNameParser.MpxDirectiveInfo
                                                          && info.directiveKind === CUSTOM),
                                     priority = LOW)
  }

  private fun resolveToProp(context: XmlTag, propName: String, attributeName: String): XmlAttributeDescriptor? {
    val propFromAsset = fromAsset(propName)
    return sources.asSequence()
      .mapNotNull {
        var result: XmlAttributeDescriptor? = null
        it.acceptPropertiesAndMethods(object : MpxModelVisitor() {
          override fun visitInputProperty(prop: MpxInputProperty, proximity: Proximity): Boolean {
            if (propFromAsset == fromAsset(prop.name)) {
              result = MpxAttributeDescriptor(context, attributeName, prop)
              return false
            }
            return true
          }
        })
        result
      }
      .firstOrNull()
  }

  override fun getAttributeDescriptor(attribute: XmlAttribute?): XmlAttributeDescriptor? =
    getAttributeDescriptor(attribute?.name, attribute?.parent)

  override fun getNSDescriptor(): XmlNSDescriptor? = null
  override fun getTopGroup(): XmlElementsGroup? = null
  override fun getContentType(): Int = XmlElementDescriptor.CONTENT_TYPE_ANY
  override fun getDefaultValue(): String? = null
}
