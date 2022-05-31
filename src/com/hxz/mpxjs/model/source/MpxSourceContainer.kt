// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.codeInsight.getTextIfLiteral
import com.hxz.mpxjs.model.*
import com.hxz.mpxjs.model.source.MpxContainerInfoProvider.MpxContainerInfo

abstract class MpxSourceContainer(sourceElement: JSImplicitElement,
                                  override val descriptor: MpxSourceEntityDescriptor)
  : UserDataHolderBase(), MpxContainer, MpxSourceEntity {

  override val source: PsiElement = sourceElement
  override val parents: List<MpxEntitiesContainer> get() = MpxGlobalImpl.getParents(this)

  override val element: String?
    get() = getTextIfLiteral(
      descriptor.initializer?.castSafelyTo<JSObjectLiteralExpression>()
        ?.findProperty(EL_PROP)?.literalExpressionInitializer)

  override val data: List<MpxDataProperty> get() = get(DATA)
  override val computed: List<MpxComputedProperty> get() = get(COMPUTED)
  override val methods: List<MpxMethod> get() = get(METHODS)
  override val props: List<MpxInputProperty> get() = get(PROPS)

  override val model: MpxModelDirectiveProperties get() = get(MODEL)

  override val emits: List<MpxEmitCall> get() = get(EMITS)
  override val template: MpxTemplate<*>? get() = if (this is MpxRegularComponent) get(TEMPLATE) else null
  override val slots: List<MpxSlot> = emptyList()

  override val delimiters: Pair<String, String>? get() = get(DELIMITERS)
  override val extends: List<MpxContainer> get() = get(EXTENDS)
  override val components: Map<String, MpxComponent> get() = get(COMPONENTS)
  override val directives: Map<String, MpxDirective> get() = get(DIRECTIVES)
  override val mixins: List<MpxMixin> get() = get(MIXINS)
  override val filters: Map<String, MpxFilter> get() = get(FILTERS)

  private fun <T> get(accessor: MemberAccessor<T>): T {
    return accessor.get(descriptor)
  }

  companion object {
    private val EXTENDS = ListAccessor(MpxContainerInfo::extends)
    private val MIXINS = ListAccessor(MpxContainerInfo::mixins)
    private val DIRECTIVES = MapAccessor(MpxContainerInfo::directives)
    private val COMPONENTS = MapAccessor(MpxContainerInfo::components)
    private val FILTERS = MapAccessor(MpxContainerInfo::filters)
    private val DELIMITERS = DelimitersAccessor(MpxContainerInfo::delimiters)

    private val PROPS = NamedListAccessor(MpxContainerInfo::props)
    private val DATA = NamedListAccessor(MpxContainerInfo::data)
    private val COMPUTED = NamedListAccessor(MpxContainerInfo::computed)
    private val METHODS = NamedListAccessor(MpxContainerInfo::methods)

    private val EMITS = NamedListAccessor(MpxContainerInfo::emits)

    private val MODEL = ModelAccessor(MpxContainerInfo::model)
    private val TEMPLATE = TemplateAccessor(MpxContainerInfo::template)

    fun getTemplate(descriptor: MpxSourceEntityDescriptor): MpxTemplate<*>? {
      return TEMPLATE.get(descriptor)
    }
  }

  private abstract class MemberAccessor<T>(val extInfoAccessor: (MpxContainerInfo) -> T?, val takeFirst: Boolean = false) {

    fun get(descriptor: MpxSourceEntityDescriptor): T {
      descriptor.ensureValid()
      return MpxContainerInfoProvider.getProviders()
               .asSequence()
               .mapNotNull { it.getInfo(descriptor)?.let(extInfoAccessor) }
               .let {
                 if (takeFirst) it.firstOrNull()
                 else it.reduceOrNull(::merge)
               }
             ?: empty()
    }

    protected abstract fun empty(): T

    protected open fun merge(arg1: T, arg2: T): T {
      throw UnsupportedOperationException()
    }

  }

  private open class ListAccessor<T>(extInfoAccessor: (MpxContainerInfo) -> List<T>)
    : MemberAccessor<List<T>>(extInfoAccessor) {
    override fun empty(): List<T> {
      return emptyList()
    }

    override fun merge(arg1: List<T>, arg2: List<T>): List<T> {
      if (arg1.isEmpty()) return arg2
      if (arg2.isEmpty()) return arg1
      return arg1.asSequence().plus(arg2).distinctBy(::keyExtractor).toList()
    }

    open fun keyExtractor(obj: T): Any {
      return obj!!
    }

  }

  private class MapAccessor<T>(extInfoAccessor: (MpxContainerInfo) -> Map<String, T>)
    : MemberAccessor<Map<String, T>>(extInfoAccessor) {

    override fun empty(): Map<String, T> {
      return emptyMap()
    }

    override fun merge(arg1: Map<String, T>, arg2: Map<String, T>): Map<String, T> {
      if (arg1.isEmpty()) return arg2
      if (arg2.isEmpty()) return arg1
      val result = arg1.toMutableMap()
      arg2.forEach { (key, value) -> result.putIfAbsent(key, value) }
      return result
    }
  }

  private class NamedListAccessor<T : MpxNamedSymbol>(extInfoAccessor: (MpxContainerInfo) -> List<T>)
    : ListAccessor<T>(extInfoAccessor) {

    override fun keyExtractor(obj: T): Any {
      return obj.name
    }
  }

  private class ModelAccessor(extInfoAccessor: (MpxContainerInfo) -> MpxModelDirectiveProperties?)
    : MemberAccessor<MpxModelDirectiveProperties>(extInfoAccessor) {

    override fun empty(): MpxModelDirectiveProperties {
      return MpxModelDirectiveProperties()
    }

    override fun merge(arg1: MpxModelDirectiveProperties, arg2: MpxModelDirectiveProperties): MpxModelDirectiveProperties {
      return arg2
    }
  }

  private class TemplateAccessor(extInfoAccessor: (MpxContainerInfo) -> MpxTemplate<*>?)
    : MemberAccessor<MpxTemplate<*>?>(extInfoAccessor, true) {

    override fun empty(): MpxTemplate<*>? {
      return null
    }
  }

  private class DelimitersAccessor(extInfoAccessor: (MpxContainerInfo) -> Pair<String, String>?)
    : MemberAccessor<Pair<String, String>?>(extInfoAccessor, true) {

    override fun empty(): Pair<String, String>? {
      return null
    }
  }

}
