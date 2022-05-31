// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import one.util.streamex.EntryStream
import com.hxz.mpxjs.codeInsight.fromAsset
import com.hxz.mpxjs.codeInsight.toAsset
import com.hxz.mpxjs.index.*
import com.hxz.mpxjs.model.*
import java.util.*

class MpxSourceGlobal(override val project: Project, private val packageJsonUrl: String?) : MpxGlobal {

  override val global: MpxGlobal = this
  override val plugins: List<MpxPlugin>
    get() = emptyList()
  override val source: PsiElement? = null
  override val parents: List<MpxEntitiesContainer> = emptyList()

  override val directives: Map<String, MpxDirective>
    get() = getCachedValue { buildDirectives(it) }
  override val filters: Map<String, MpxFilter>
    get() = getCachedValue { buildFiltersMap(it) }
  override val apps: List<MpxApp>
    get() = getCachedValue { buildAppsList(it) }
  override val mixins: List<MpxMixin>
    get() = getCachedValue { buildMixinsList(it) }

  override val components: Map<String, MpxComponent>
    get() = getComponents(true)

  override val unregistered: MpxEntitiesContainer = object : MpxEntitiesContainer {
    override val components: Map<String, MpxComponent>
      get() = getComponents(false)
    override val directives: Map<String, MpxDirective> = emptyMap()
    override val filters: Map<String, MpxFilter> = emptyMap()
    override val mixins: List<MpxMixin> get() = emptyList()
    override val source: PsiElement? = null
    override val parents: List<MpxEntitiesContainer> = emptyList()
  }

  override fun equals(other: Any?): Boolean =
    (other as? MpxSourceGlobal)?.let {
      it.project == project && it.packageJsonUrl == packageJsonUrl
    } ?: false

  override fun hashCode(): Int = (project.hashCode()) * 31 + packageJsonUrl.hashCode()

  private fun getComponents(global: Boolean): Map<String, MpxComponent> =
    getCachedValue { scope ->
      val componentsData = MpxComponentsCalculation.calculateScopeComponents(scope, false)

      val moduleComponents = componentsData.map

      val localComponents: MutableMap<String, MpxComponent> = EntryStream.of(moduleComponents)
        .filterValues { !it.second }
        .mapValues { MpxModelManager.getComponent(it.first) }
        .nonNullValues()
        // TODO properly support multiple components with the same name
        .distinctKeys()
        .into(sortedMapOf())

      val globalComponents: MutableMap<String, MpxComponent> = EntryStream.of(moduleComponents)
        .filterValues { it.second }
        .mapValues { MpxModelManager.getComponent(it.first) }
        .nonNullValues()
        // TODO properly support multiple components with the same name
        .distinctKeys()
        .into(sortedMapOf())

      componentsData.libCompResolveMap.forEach { (alias, target) ->
        localComponents[target]?.let { localComponents.putIfAbsent(alias, it) }
        globalComponents[target]?.let { globalComponents.putIfAbsent(alias, it) }
      }

      // Add Mpx files without regular initializer as possible imports
      val psiManager = PsiManager.getInstance(project)
      FileBasedIndex.getInstance().getFilesWithKey(
        MpxEmptyComponentInitializersIndex.MPX_NO_INITIALIZER_COMPONENTS_INDEX, setOf(true),
        { file ->
          val componentName = fromAsset(file.nameWithoutExtension)
          if (!localComponents.containsKey(componentName)) {
            psiManager.findFile(file)
              ?.let { psiFile ->
                MpxModelManager.getComponent(MpxSourceEntityDescriptor(source = psiFile))
              }
              ?.let { localComponents[componentName] = it }
          }
          true
        }, scope)

      // Contribute components from providers.
      val sourceComponents = MpxContainerInfoProvider.ComponentsInfo(localComponents.toMap(), globalComponents.toMap())
      MpxContainerInfoProvider.getProviders()
        .mapNotNull { it.getAdditionalComponents(scope, sourceComponents) }
        .forEach {
          globalComponents.putAll(it.global)
          localComponents.putAll(it.local)
        }

      MpxContainerInfoProvider.ComponentsInfo(localComponents.toMap(), globalComponents.toMap())
    }.get(!global)

  private fun <T> getCachedValue(provider: (GlobalSearchScope) -> T): T {
    val psiFile: PsiFile? = MpxGlobalImpl.findFileByUrl(packageJsonUrl)
      ?.let { PsiManager.getInstance(project).findFile(it) }
    val searchScope = psiFile?.parent
                        ?.let {
                          GlobalSearchScopesCore.directoryScope(it, true)
                            .intersectWith(GlobalSearchScope.projectScope(project))
                        }
                      ?: GlobalSearchScope.projectScope(project)
    val manager = CachedValuesManager.getManager(project)
    return CachedValuesManager.getManager(project).getCachedValue(
      psiFile ?: project,
      manager.getKeyForClass(provider::class.java),
      {
        Result.create(provider(searchScope), VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                      PsiModificationTracker.MODIFICATION_COUNT)
      },
      false)
  }

  companion object {
    private fun buildDirectives(searchScope: GlobalSearchScope): Map<String, MpxDirective> =
      getForAllKeys(searchScope, MpxGlobalDirectivesIndex.KEY)
        .asSequence()
        .map { Pair(it.name, MpxSourceDirective(it.name, it.parent)) }
        // TODO properly support multiple directives with the same name
        .distinctBy { it.first }
        .toMap(TreeMap())

    private fun buildMixinsList(scope: GlobalSearchScope): List<MpxMixin> =
      resolve(GLOBAL, scope, MpxMixinBindingIndex.KEY)
        ?.asSequence()
        ?.mapNotNull { MpxComponents.mpxMixinDescriptorFinder(it) }
        ?.mapNotNull { MpxModelManager.getMixin(it) }
        ?.toList()
      ?: emptyList()

    private fun buildAppsList(scope: GlobalSearchScope): List<MpxApp> =
      getForAllKeys(scope, MpxOptionsIndex.KEY)
        .asSequence()
        .filter(MpxComponents.Companion::isNotInLibrary)
        .mapNotNull { it as? JSObjectLiteralExpression ?: PsiTreeUtil.getParentOfType(it, JSObjectLiteralExpression::class.java) }
        .map { MpxModelManager.getApp(it) }
        .filter { it.element != null }
        .toList()

    private fun buildFiltersMap(scope: GlobalSearchScope): Map<String, MpxFilter> =
      getForAllKeys(scope, MpxGlobalFiltersIndex.KEY)
        .asSequence()
        .mapNotNull { element ->
          MpxModelManager.getFilter(element)
            ?.let { Pair(toAsset(element.name), it) }
        }
        // TODO properly support multiple filters with the same name
        .distinctBy { it.first }
        .toMap(TreeMap())
  }
}
