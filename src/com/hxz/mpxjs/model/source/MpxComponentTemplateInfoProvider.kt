// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.castSafelyTo
import com.intellij.xml.util.HtmlUtil
import com.hxz.mpxjs.codeInsight.getFirstInjectedFile
import com.hxz.mpxjs.codeInsight.getHostFile
import com.hxz.mpxjs.codeInsight.getTextIfLiteral
import com.hxz.mpxjs.index.MpxUrlIndex
import com.hxz.mpxjs.index.findTopLevelMpxTag
import com.hxz.mpxjs.model.MpxFileTemplate
import com.hxz.mpxjs.model.MpxTagTemplate
import com.hxz.mpxjs.model.MpxTemplate
import com.hxz.mpxjs.model.resolveTagSrcReference

class MpxComponentTemplateInfoProvider : MpxContainerInfoProvider {

  override fun getInfo(descriptor: MpxSourceEntityDescriptor): MpxContainerInfoProvider.MpxContainerInfo? =
    MpxComponentTemplateInfo(descriptor.initializer ?: descriptor.clazz ?: descriptor.source)

  private class MpxComponentTemplateInfo(private val element: PsiElement) : MpxContainerInfoProvider.MpxContainerInfo {
    override val template: MpxTemplate<*>?
      get() {
        val element = element
        return CachedValuesManager.getCachedValue(element) {
          locateTemplateInTheSameMpxFile(element)
          ?: locateTemplateInTemplateProperty(element)
          ?: locateTemplateInReferencingMpxFile(element)
          ?: CachedValueProvider.Result.create(null as MpxTemplate<*>?, element)
        }
      }
  }

  companion object {

    private fun createInfo(template: PsiElement?): MpxTemplate<*>? =
      when (template) {
        is XmlFile -> MpxFileTemplate(template)
        is XmlTag -> MpxTagTemplate(template)
        else -> null
      }

    private fun locateTemplateInTheSameMpxFile(source: PsiElement): CachedValueProvider.Result<MpxTemplate<*>?>? {
      val context = source as? PsiFile ?: source.context!!
      return context.containingFile
        ?.castSafelyTo<XmlFile>()
        ?.let { findTopLevelMpxTag(it, HtmlUtil.TEMPLATE_TAG_NAME) }
        ?.let {
          CachedValueProvider.Result.create(locateTemplateInTemplateTag(it), context, context.containingFile)
        }
    }

    private fun locateTemplateInTemplateProperty(source: PsiElement): CachedValueProvider.Result<MpxTemplate<*>?>? =
      (source as? JSObjectLiteralExpression)
        ?.findProperty(TEMPLATE_PROP)
        ?.value
        ?.let { expression ->
          // Inline template
          getFirstInjectedFile(expression)
            ?.let { return CachedValueProvider.Result.create(createInfo(it), source, it) }

          // Referenced template
          getReferencedTemplate(expression)
        }

    private fun getReferencedTemplate(expression: JSExpression): CachedValueProvider.Result<MpxTemplate<*>?> {
      var directRefs = getTextIfLiteral(expression)?.startsWith("#") == true
      var referenceExpr = expression

      if (expression is JSCallExpression) {
        val args = expression.arguments
        if (args.size == 1) {
          referenceExpr = args[0]
          directRefs = true
        }
        else {
          return CachedValueProvider.Result.create(null, expression)
        }
      }

      var result: PsiElement? = null
      refs@ for (ref in referenceExpr.references) {
        val el = ref.resolve()
        if (directRefs) {
          if (el is PsiFile || el is XmlTag) {
            result = el
            break@refs
          }
        }
        else if (el is ES6ImportedBinding) {
          for (importedElement in el.findReferencedElements()) {
            if (importedElement is PsiFile) {
              result = importedElement
              break@refs
            }
          }
        }
      }
      return CachedValueProvider.Result.create(createInfo(result), expression, referenceExpr,
                                               VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
    }

    private fun locateTemplateInReferencingMpxFile(source: PsiElement): CachedValueProvider.Result<MpxTemplate<*>?>? {
      val file = getHostFile(source) ?: return null
      val name = file.viewProvider.virtualFile.name
      var result: CachedValueProvider.Result<MpxTemplate<*>?>? = null

      StubIndex.getInstance().processElements(MpxUrlIndex.KEY, name, source.project,
                                              GlobalSearchScope.projectScope(source.project), PsiElement::class.java) { element ->
        if (element is XmlAttribute
            && element.context?.let { it is XmlTag && it.name == HtmlUtil.SCRIPT_TAG_NAME } == true
            && element.valueElement?.references
              ?.any { it.resolve()?.containingFile == source.containingFile } == true) {
          result = CachedValueProvider.Result.create(
            element.containingFile
              ?.castSafelyTo<XmlFile>()
              ?.let { findTopLevelMpxTag(it, HtmlUtil.TEMPLATE_TAG_NAME) }
              ?.let { locateTemplateInTemplateTag(it) },
            element, element.containingFile,
            source, source.containingFile,
            VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
          )
          return@processElements false
        }
        true
      }
      return result
    }

    private fun locateTemplateInTemplateTag(tag: XmlTag): MpxTemplate<*>? =
      resolveTagSrcReference(tag)?.let { createInfo(it) }

  }

}
