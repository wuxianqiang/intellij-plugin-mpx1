// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

interface MpxEntitiesContainer : MpxScopeElement, MpxInstanceOwner {
  val components: Map<String, MpxComponent>
  val directives: Map<String, MpxDirective>
  val filters: Map<String, MpxFilter>
  val mixins: List<MpxMixin>
}
