// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.javascript.nodejs.library.NodeModulesDirectoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.hxz.mpxjs.index.BOOTSTRAP_MPX_MODULE
import com.hxz.mpxjs.index.SHARDS_MPX_MODULE
import com.hxz.mpxjs.index.MPXTIFY_MODULE
import com.hxz.mpxjs.model.*

class MpxSourcePlugin constructor(private val project: Project,
                                  override val moduleName: String?,
                                  override val moduleVersion: String?,
                                  private val packageJsonFile: VirtualFile) : UserDataHolderBase(), MpxPlugin {

  override val parents: List<MpxEntitiesContainer> = emptyList()

  override val directives: Map<String, MpxDirective> = emptyMap()
  override val filters: Map<String, MpxFilter> = emptyMap()
  override val mixins: List<MpxMixin> = emptyList()

  override val source: PsiDirectory?
    get() = PsiManager.getInstance(project).findFile(packageJsonFile)?.parent

  override val components: Map<String, MpxComponent>
    get() = CachedValuesManager.getManager(project).getCachedValue(this) {
      val dependencies = mutableListOf<Any>(NodeModulesDirectoryManager.getInstance(project).nodeModulesDirChangeTracker,
                                            packageJsonFile)
      val psiDirectory = source
      val components: Map<String, MpxComponent>
      if (psiDirectory == null) {
        components = emptyMap()
        dependencies.add(PsiModificationTracker.MODIFICATION_COUNT)
      }
      else {
        val directoryFile = psiDirectory.virtualFile
        val scope = GlobalSearchScopesCore.directoryScope(psiDirectory.project, directoryFile, true)
        val globalize = PACKAGES_WITH_GLOBAL_COMPONENTS.contains(psiDirectory.name)

        if (directoryFile.`is`(VFileProperty.SYMLINK)) {
          // Track modifications in plugins ASTs only if they are possibly local
          dependencies.add(PsiModificationTracker.MODIFICATION_COUNT)
        }

        components = MpxComponentsCalculation.calculateScopeComponents(scope, globalize).map.asSequence()
          .mapNotNull { MpxModelManager.getComponent(it.value.first)?.let { component -> Pair(it.key, component) } }
          .distinctBy { it.first }
          .toMap()
      }
      CachedValueProvider.Result(components, *dependencies.toTypedArray())
    } ?: emptyMap()

  override fun equals(other: Any?): Boolean {
    return (other as? MpxSourcePlugin)?.packageJsonFile == packageJsonFile
           && other.project == project
  }

  override fun hashCode(): Int {
    var result = project.hashCode()
    result = 31 * result + packageJsonFile.hashCode()
    return result
  }

  companion object {
    private val PACKAGES_WITH_GLOBAL_COMPONENTS = arrayOf(MPXTIFY_MODULE, BOOTSTRAP_MPX_MODULE, SHARDS_MPX_MODULE)
  }
}
