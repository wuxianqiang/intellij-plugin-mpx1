// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSElement
import com.intellij.psi.util.PsiTreeUtil

interface MpxSourceEntity {

  fun isPartOfImplementation(element: JSElement): Boolean =
    PsiTreeUtil.isContextAncestor(initializer, element, false)
    || PsiTreeUtil.isContextAncestor(descriptor.clazz, element, false)

  val descriptor: MpxSourceEntityDescriptor
  val initializer: JSElement? get() = descriptor.initializer
}