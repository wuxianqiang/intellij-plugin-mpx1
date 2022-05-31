// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

import com.intellij.openapi.util.UserDataHolderBase

abstract class MpxDelegatedEntitiesContainer<T : MpxEntitiesContainer> : UserDataHolderBase(), MpxEntitiesContainer {

  protected abstract val delegate: T

  override val components: Map<String, MpxComponent> get() = delegate.components
  override val directives: Map<String, MpxDirective> get() = delegate.directives
  override val filters: Map<String, MpxFilter> get() = delegate.filters
  override val mixins: List<MpxMixin> get() = delegate.mixins
}
