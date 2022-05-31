// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSRecordType.PropertySignature
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.search.GlobalSearchScope
import com.hxz.mpxjs.model.*
import com.hxz.mpxjs.model.source.EntityContainerInfoProvider.DecoratedContainerInfoProvider
import com.hxz.mpxjs.model.source.EntityContainerInfoProvider.InitializedContainerInfoProvider
import com.hxz.mpxjs.model.source.MpxContainerInfoProvider.MpxContainerInfo

interface MpxContainerInfoProvider : EntityContainerInfoProvider<MpxContainerInfo> {

  @JvmDefault
  fun getAdditionalComponents(scope: GlobalSearchScope, sourceComponents: ComponentsInfo): ComponentsInfo? = null

  @JvmDefault
  fun getThisTypeProperties(instanceOwner: MpxInstanceOwner,
                            standardProperties: MutableMap<String, PropertySignature>)
    : Collection<PropertySignature> = emptyList()

  data class ComponentsInfo(val local: Map<String, MpxComponent>, val global: Map<String, MpxComponent>) {
    fun get(local: Boolean): Map<String, MpxComponent> = if (local) this.local else global
  }

  interface MpxContainerInfo {
    val components: Map<String, MpxComponent> get() = emptyMap()
    val directives: Map<String, MpxDirective> get() = emptyMap()
    val filters: Map<String, MpxFilter> get() = emptyMap()
    val mixins: List<MpxMixin> get() = emptyList()
    val extends: List<MpxMixin> get() = emptyList()

    val data: List<MpxDataProperty> get() = emptyList()
    val props: List<MpxInputProperty> get() = emptyList()
    val computed: List<MpxComputedProperty> get() = emptyList()
    val methods: List<MpxMethod> get() = emptyList()
    val emits: List<MpxEmitCall> get() = emptyList()

    val model: MpxModelDirectiveProperties? get() = null
    val template: MpxTemplate<*>? get() = null
    val delimiters: Pair<String, String>? get() = null
  }

  companion object {
    private val EP_NAME = ExtensionPointName.create<MpxContainerInfoProvider>("com.intellij.mpxjs.containerInfoProvider")

    fun getProviders(): List<MpxContainerInfoProvider> = EP_NAME.extensionList
  }


  abstract class MpxDecoratedContainerInfoProvider(createInfo: (clazz: JSClass) -> MpxContainerInfo)
    : DecoratedContainerInfoProvider<MpxContainerInfo>(createInfo), MpxContainerInfoProvider

  abstract class MpxInitializedContainerInfoProvider(createInfo: (initializer: JSElement) -> MpxContainerInfo)
    : InitializedContainerInfoProvider<MpxContainerInfo>(createInfo), MpxContainerInfoProvider {

    protected abstract class MpxInitializedContainerInfo(declaration: JSElement)
      : InitializedContainerInfo(declaration), MpxContainerInfo

  }
}
