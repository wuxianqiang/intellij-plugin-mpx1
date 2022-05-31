// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.typescript.service

import com.intellij.lang.javascript.DialectDetector
import com.intellij.lang.javascript.integration.JSAnnotationError
import com.intellij.lang.javascript.service.JSLanguageServiceAnnotationResult
import com.intellij.lang.javascript.service.JSLanguageServiceFileCommandCache
import com.intellij.lang.javascript.service.protocol.JSLanguageServiceObject
import com.intellij.lang.javascript.service.protocol.JSLanguageServiceProtocol
import com.intellij.lang.javascript.service.protocol.JSLanguageServiceSimpleCommand
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceAnnotationResult
import com.intellij.lang.typescript.compiler.languageService.TypeScriptServerServiceImpl
import com.intellij.lang.typescript.compiler.languageService.codeFixes.TypeScriptLanguageServiceFixSet
import com.intellij.lang.typescript.compiler.languageService.protocol.TypeScriptLanguageServiceCache
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.ConfigureRequest
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.ConfigureRequestArguments
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.FileExtensionInfo
import com.intellij.lang.typescript.tsconfig.TypeScriptConfigService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.index.findModule
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.typescript.service.protocol.MpxTypeScriptServiceProtocol

class MpxTypeScriptService(project: Project) : TypeScriptServerServiceImpl(project, "Mpx Console") {

  override fun isAcceptableNonTsFile(project: Project, service: TypeScriptConfigService, virtualFile: VirtualFile): Boolean {
    if (super.isAcceptableNonTsFile(project, service, virtualFile)) return true
    if (!isMpxFile(virtualFile)) return false

    return service.getDirectIncludePreferableConfig(virtualFile) != null
  }

  override fun postprocessErrors(file: PsiFile, errors: MutableList<JSAnnotationError>): List<JSAnnotationError> {
    if (file.virtualFile != null && isMpxFile(file.virtualFile)) {
      return ReadAction.compute<List<JSAnnotationError>, Throwable> {
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file)
        val module = findModule(file)
        if (module != null && document != null) {
          val startOffset = module.textRange.startOffset
          val startLine = document.getLineNumber(startOffset)
          val startColumn = startOffset - document.getLineStartOffset(startLine)
          val endOffset = module.textRange.endOffset
          val endLine = document.getLineNumber(endOffset)
          val endColumn = endOffset - document.getLineStartOffset(endLine)
          return@compute errors.filter { error -> isWithinRange(error, startLine, startColumn, endLine, endColumn) }
        }
        return@compute super.postprocessErrors(file, errors)
      }
    }
    return super.postprocessErrors(file, errors)
  }

  private fun isWithinRange(error: JSAnnotationError, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Boolean {
    if (error !is JSLanguageServiceAnnotationResult) {
      return false
    }
    return (error.line > startLine || error.line == startLine && error.column >= startColumn) &&
           (error.endLine < endLine || error.endLine == endLine && error.endColumn <= endColumn)
  }

  override fun getProcessName(): String = "Mpx TypeScript"

  override fun isServiceEnabled(context: VirtualFile): Boolean {
    //if (!super.isServiceEnabled(context)) return false
    if (context.fileType is MpxFileType) return true

    //other files
    return isMpxContext(context, myProject)
  }

  override fun createProtocol(readyConsumer: Consumer<*>, tsServicePath: String): JSLanguageServiceProtocol {
    return MpxTypeScriptServiceProtocol(myProject, mySettings, readyConsumer, createEventConsumer(), tsServicePath)
  }

  override fun getInitialCommands(): Map<JSLanguageServiceSimpleCommand, Consumer<JSLanguageServiceObject>> {
    //commands
    val initialCommands = super.getInitialCommands()
    val result: MutableMap<JSLanguageServiceSimpleCommand, Consumer<JSLanguageServiceObject>> = linkedMapOf()
    addConfigureCommand(result)

    result.putAll(initialCommands)
    return result
  }

  override fun canHighlight(file: PsiFile): Boolean {
    if (super.canHighlight(file)) return true

    val fileType = file.fileType
    if (fileType != MpxFileType.INSTANCE) return false

    val virtualFile = file.virtualFile ?: return false

    if (!isServiceEnabled(virtualFile) || !checkAnnotationProvider(file)) return false

    val module = findModule(file)
    if (module == null || !DialectDetector.isTypeScript(module)) return false

    val configForFile = getConfigForFile(virtualFile)

    return configForFile != null
  }

  private fun addConfigureCommand(result: MutableMap<JSLanguageServiceSimpleCommand, Consumer<JSLanguageServiceObject>>) {
    val arguments = ConfigureRequestArguments()
    val fileExtensionInfo = FileExtensionInfo()
    fileExtensionInfo.extension = ".mpx"

    //see ts.getSupportedExtensions
    //x.scriptKind === ScriptKind.Deferred(7) || needJsExtensions && isJSLike(x.scriptKind) ? x.extension : undefined
    //so only "ScriptKind.Deferred" kinds are accepted for file searching
    fileExtensionInfo.scriptKind = 7

    fileExtensionInfo.isMixedContent = false
    arguments.extraFileExtensions = arrayOf(fileExtensionInfo)

    result[ConfigureRequest(arguments)] = Consumer {}
  }

  override fun createLSCache(): TypeScriptLanguageServiceCache {
    return MpxTypeScriptServiceCache(myProject)
  }

  override fun createFixSet(file: PsiFile,
                            cache: JSLanguageServiceFileCommandCache,
                            typescriptResult: TypeScriptLanguageServiceAnnotationResult): TypeScriptLanguageServiceFixSet {
    val textRange = findModule(file)?.textRange
    return TypeScriptLanguageServiceFixSet(file.project, cache, file.virtualFile, typescriptResult, textRange)
  }

  private fun isMpxFile(virtualFile: VirtualFile) = virtualFile.fileType == MpxFileType.INSTANCE

}
