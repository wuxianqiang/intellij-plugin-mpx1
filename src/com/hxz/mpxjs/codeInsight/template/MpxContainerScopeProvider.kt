// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.template

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.hxz.mpxjs.model.MpxEntitiesContainer
import com.hxz.mpxjs.model.MpxModelManager
import java.util.function.Consumer

class MpxContainerScopeProvider : MpxTemplateScopesProvider() {

  override fun getScopes(element: PsiElement, hostElement: PsiElement?): List<MpxTemplateScope> {
    return MpxModelManager.findEnclosingContainer(hostElement ?: element)
             ?.let { listOf(MpxContainerScope(it)) }
           ?: emptyList()
  }

  private class MpxContainerScope constructor(private val myEntitiesContainer: MpxEntitiesContainer)
    : MpxTemplateScope(null) {

    override fun resolve(consumer: Consumer<in ResolveResult>) {
      myEntitiesContainer.thisType
        .asRecordType()
        .properties
        .asSequence()
        .mapNotNull { it.memberSource.singleElement }
        .map { PsiElementResolveResult(it, true) }
        .forEach { consumer.accept(it) }
    }
  }
}
