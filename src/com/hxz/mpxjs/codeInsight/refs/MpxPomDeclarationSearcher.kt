// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.refs

import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.Consumer
import com.hxz.mpxjs.lang.html.psi.MpxRefAttribute

class MpxPomDeclarationSearcher : PomDeclarationSearcher() {

  override fun findDeclarationsAt(element: PsiElement, offsetInElement: Int, consumer: Consumer<PomTarget>) {
    if (element is XmlAttributeValue && element.parent is MpxRefAttribute
        && ElementManipulators.getValueTextRange(element).contains(offsetInElement)) {
      (element.parent as MpxRefAttribute).implicitElement?.let { consumer.consume(it) }
    }
  }

}
