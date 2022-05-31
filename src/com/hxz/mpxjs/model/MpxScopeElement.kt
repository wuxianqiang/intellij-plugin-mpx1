// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

import com.intellij.psi.PsiElement
import com.hxz.mpxjs.codeInsight.fromAsset

interface MpxScopeElement {

  val source: PsiElement?

  val parents: List<MpxEntitiesContainer>

  val global: MpxGlobal?
    get() {
      return source?.let { MpxModelManager.getGlobal(it) }
    }

  fun acceptEntities(visitor: MpxModelVisitor,
                     minimumProximity: MpxModelVisitor.Proximity = MpxModelVisitor.Proximity.GLOBAL): Boolean {
    val visited = mutableSetOf<Pair<String, MpxScopeElement>>()
    val containersStack = mutableListOf<Pair<MpxEntitiesContainer, MpxModelVisitor.Proximity>>()

    if (minimumProximity <= MpxModelVisitor.Proximity.GLOBAL) {
      global?.let {
        containersStack.add(Pair(it, MpxModelVisitor.Proximity.GLOBAL))
        it.plugins.forEach { plugin -> containersStack.add(Pair(plugin, MpxModelVisitor.Proximity.GLOBAL)) }
        if (minimumProximity <= MpxModelVisitor.Proximity.OUT_OF_SCOPE) {
          containersStack.add(Pair(it.unregistered, MpxModelVisitor.Proximity.OUT_OF_SCOPE))
        }
      }
    }

    if (minimumProximity <= MpxModelVisitor.Proximity.PLUGIN) {
      parents.forEach { parent ->
        when (parent) {
          is MpxApp -> if (minimumProximity <= MpxModelVisitor.Proximity.APP)
            containersStack.add(Pair(parent, MpxModelVisitor.Proximity.APP))
          is MpxPlugin -> if (minimumProximity <= MpxModelVisitor.Proximity.PLUGIN)
            containersStack.add(Pair(parent, MpxModelVisitor.Proximity.PLUGIN))
        }
      }
    }

    if (this is MpxEntitiesContainer) {
      containersStack.add(Pair(this, MpxModelVisitor.Proximity.LOCAL))
    }

    containersStack.sortBy { it.second }

    while (containersStack.isNotEmpty()) {
      val (container, proximity) = containersStack.removeAt(containersStack.size - 1)

      if (!visited.add(Pair("", container))) continue

      if ((container is MpxMixin
           && !visitor.visitMixin(container, proximity))
          || (container is MpxComponent
              && !visitor.visitSelfComponent(container, proximity))
          || (container is MpxApp
              && proximity == MpxModelVisitor.Proximity.LOCAL
              && !visitor.visitSelfApplication(container, proximity))) {
        return false
      }

      ((container as? MpxContainer)?.extends)?.forEach {
        containersStack.add(Pair(it, proximity))
      }
      container.mixins.forEach { mixin ->
        containersStack.add(Pair(mixin, proximity))
      }
      container.components.forEach { (name, component) ->
        if (visited.add(Pair(fromAsset(name), component))
            && !visitor.visitComponent(name, component, proximity)) {
          return false
        }
      }
      container.directives.forEach { (name, directive) ->
        if (visited.add(Pair(fromAsset(name), directive))
            && !visitor.visitDirective(name, directive, proximity)) {
          return false
        }
      }
      container.filters.forEach { (name, filter) ->
        if (visited.add(Pair(name, filter))
            && !visitor.visitFilter(name, filter, proximity)) {
          return false
        }
      }
    }
    return true
  }

  fun acceptPropertiesAndMethods(visitor: MpxModelVisitor, onlyPublic: Boolean = true) {
    acceptEntities(object : MpxModelVisitor() {
      override fun visitSelfComponent(component: MpxComponent, proximity: Proximity): Boolean {
        return if (component is MpxContainer) visitContainer(component, proximity) else true
      }

      override fun visitSelfApplication(application: MpxApp, proximity: Proximity): Boolean {
        return visitContainer(application, proximity)
      }

      override fun visitMixin(mixin: MpxMixin, proximity: Proximity): Boolean {
        return visitContainer(mixin, proximity)
      }

      fun visitContainer(container: MpxContainer, proximity: Proximity): Boolean {
        return container.props.all { visitor.visitInputProperty(it, proximity) }
               && (onlyPublic
                   || (container.data.all { visitor.visitDataProperty(it, proximity) }
                       && container.computed.all { visitor.visitComputedProperty(it, proximity) }
                       && container.methods.all { visitor.visitMethod(it, proximity) }
                      ))
      }
    }, MpxModelVisitor.Proximity.GLOBAL)
  }

}
