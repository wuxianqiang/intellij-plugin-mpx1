// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.refactoring

import com.intellij.lang.javascript.psi.impl.JSPropertyImpl
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.hxz.mpxjs.index.MpxComponentsIndex
import com.hxz.mpxjs.index.resolve
import com.hxz.mpxjs.lang.html.MpxFileType

object MpxRefactoringUtils {
  fun getComponent(element: PsiElement): JSImplicitElement? {
    if (element is JSImplicitElementImpl) {
      return resolve(element.name, GlobalSearchScope.projectScope(element.project), MpxComponentsIndex.KEY)?.first()
    }
    return null
  }

  fun isComponentName(element: PsiElement): Boolean {
    if (element.containingFile == null) return false
    if (element.containingFile.fileType != MpxFileType.INSTANCE) return false
    return ((element.parent.parent as? JSPropertyImpl)?.name == "name")
  }
}
