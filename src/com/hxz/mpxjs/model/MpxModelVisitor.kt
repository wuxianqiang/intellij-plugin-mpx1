// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

abstract class MpxModelVisitor {

  open fun visitComponent(name: String, component: MpxComponent, proximity: Proximity): Boolean {
    return true
  }

  open fun visitSelfComponent(component: MpxComponent, proximity: Proximity): Boolean {
    return component.defaultName?.let { visitComponent(it, component, proximity) } ?: true
  }

  open fun visitSelfApplication(application: MpxApp, proximity: Proximity): Boolean {
    return true
  }

  open fun visitMixin(mixin: MpxMixin, proximity: Proximity): Boolean {
    return true
  }

  open fun visitFilter(name: String, filter: MpxFilter, proximity: Proximity): Boolean {
    return true
  }

  open fun visitDirective(name: String, directive: MpxDirective, proximity: Proximity): Boolean {
    return true
  }

  open fun visitProperty(property: MpxProperty, proximity: Proximity): Boolean {
    return true
  }

  open fun visitInputProperty(prop: MpxInputProperty, proximity: Proximity): Boolean {
    return visitProperty(prop, proximity)
  }

  open fun visitComputedProperty(computedProperty: MpxComputedProperty, proximity: Proximity): Boolean {
    return visitProperty(computedProperty, proximity)
  }

  open fun visitDataProperty(dataProperty: MpxDataProperty, proximity: Proximity): Boolean {
    return visitProperty(dataProperty, proximity)
  }

  open fun visitMethod(method: MpxMethod, proximity: Proximity): Boolean {
    return visitProperty(method, proximity)
  }

  enum class Proximity {
    OUT_OF_SCOPE,
    GLOBAL,
    APP,
    PLUGIN,
    LOCAL
  }

}
