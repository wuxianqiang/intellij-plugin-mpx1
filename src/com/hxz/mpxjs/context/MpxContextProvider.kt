// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.context

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.annotations.ApiStatus

//@ApiStatus.ScheduledForRemoval(inVersion = "2022.1")
@Deprecated(message = "Use WebFrameworkContext API instead.", level = DeprecationLevel.WARNING)
interface MpxContextProvider {
  /**
   * Context providers can determine whether a particular, parsed file should have Mpx support enabled.
   * In such files Mpx expressions will be injected. This API serves for a purpose of enabling Mpx.js
   * support in particular files, when Mpx support should not be provided on a directory level.
   * Results are or'ed together. It is a responsibility of the context provider to cache value if needed.
   */
  @JvmDefault
  fun isMpxContextEnabled(file: PsiFile): Boolean = false

  /**
   * Context providers can determine whether files within a particular folder
   * should have Mpx support enabled. Html files in such folders will be parsed with Mpx parser, so
   * Mpx.js expressions will be part of PSI tress instead of being injected. Results are or'ed together.
   * It is a responsibility of the context provider to include all dependencies of the value.
   * The result is being cached.
   *
   * It is important that result is stable as any change will result in full reload of code insight
   * and clear of all caches.
   */
  @JvmDefault
  fun isMpxContext(directory: PsiDirectory): CachedValueProvider.Result<Boolean> =
    CachedValueProvider.Result(false, ModificationTracker.NEVER_CHANGED)

  /**
   * Context providers can forbid Mpx context on a particular file to allow for cooperation between different
   * plugins. This method is used before creating a PsiFile, so it should not try to use PsiManager to find a PsiFile.
   * The result is not cached and therefore the logic should not perform time consuming tasks, or should cache results
   * on it's own.
   *
   * It is important that result is stable as any change will result in full reload of code insight
   * and clear of all caches.
   */
  @JvmDefault
  fun isMpxContextForbidden(contextFile: VirtualFile, project: Project): Boolean = false

  companion object {
    @Suppress("DEPRECATION")
    //@ApiStatus.ScheduledForRemoval(inVersion = "2022.1")
    @Deprecated(message = "Use WebFrameworkContext API instead.", level = DeprecationLevel.WARNING)
    val MPX_CONTEXT_PROVIDER_EP = ExtensionPointName.create<MpxContextProvider>("com.intellij.mpxjs.contextProvider")
  }
}
