// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.DialectDetector
import com.intellij.lang.javascript.frameworks.JSFrameworkSpecificHandler
import com.intellij.lang.javascript.psi.JSExpectedTypeKind
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.javascript.psi.types.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.HtmlUtil
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.index.MPX_MODULE
import com.hxz.mpxjs.index.MpxFrameworkHandler
import com.hxz.mpxjs.lang.expr.MpxJSLanguage
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.model.MpxModelManager
import com.hxz.mpxjs.model.source.COMPUTED_PROP
import com.hxz.mpxjs.model.source.METHODS_PROP
import com.hxz.mpxjs.model.source.WATCH_PROP

class MpxFrameworkInsideScriptSpecificHandler : JSFrameworkSpecificHandler {
  companion object {
    fun isInsideScript(element: PsiElement): Boolean {
      val tag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return false
      return HtmlUtil.isScriptTag(tag)
    }
  }

  override fun findExpectedType(parent: PsiElement, expectedTypeKind: JSExpectedTypeKind): JSType? {
    if (parent is JSObjectLiteralExpression) {
      val language = DialectDetector.languageOfElement(parent)
      if ((MpxFileType.INSTANCE == parent.containingFile?.fileType
           && isInsideScript(parent)
           && MpxJSLanguage.INSTANCE != language
           && (MpxFrameworkHandler.hasComponentIndicatorProperties(parent) || parent.context is ES6ExportDefaultAssignment))
          || (parent.containingFile is JSFile && MpxFrameworkHandler.hasComponentIndicatorProperties(parent)
              && isMpxContext(parent))) {
        return getObjectLiteralTypeForComponent(parent)
      }
    }
    return null
  }

  private fun getObjectLiteralTypeForComponent(obj: JSObjectLiteralExpression): JSType? {
    /* The information about `this` type is processed only in TS context. */
    val thisType = MpxModelManager.findEnclosingComponent(obj)
      ?.thisType
      ?.let {
        JSGenericTypeImpl(it.source, JSNamedTypeFactory.createType("ThisType", it.source, JSTypeContext.INSTANCE), it)
      }
    val propertiesWithThisType =
      if (thisType != null)
        JSRecordTypeImpl(thisType.source, sequenceOf(COMPUTED_PROP, METHODS_PROP, WATCH_PROP)
          .map { JSRecordTypeImpl.PropertySignatureImpl(it, thisType, true, false) }
          .toList()
        )
      else null
    val definitionType = resolveSymbolFromNodeModule(obj, MPX_MODULE, "Component", TypeScriptTypeAlias::class.java)
      ?.typeDeclaration
      ?.jsType
    return listOfNotNull(definitionType, thisType, propertiesWithThisType)
      .takeIf { it.isNotEmpty() }
      ?.let {
        JSCompositeTypeFactory.createUnionType(it[0].source, it)
      }
      ?.let {
        if (it is JSUnionType) {
          JSCompositeTypeFactory.createContextualUnionType(it.types, it.source)
        }
        else it
      }
  }

}
