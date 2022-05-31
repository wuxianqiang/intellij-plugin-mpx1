// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import * as ts from "typescript/lib/tsserverlibrary";
import {MpxScriptCache} from "./mpxScriptCache"

let patched = false;

module.exports = function init(
  {typescript: ts_impl}: { typescript: typeof ts },
) {
  if (!patched) {
    patched = true

    // Don't output compilation results of `.mpx` files. The DefaultSessionExtension#getFileWrite should actually be patched
    // to not populate the output list
    const _writeFile = ts_impl.sys.writeFile
    ts_impl.sys.writeFile = function writeFilePatched(path: string, data: string, writeByteOrderMark?: boolean) {
      if (path.endsWith(".mpx.d.ts") || path.endsWith(".mpx.js") || path.endsWith(".mpx.js.map")) {
        return
      }
      _writeFile(path, data, writeByteOrderMark);
    }

    // Detect whether script kind has changed and if so, drop the whole program and patch compiler host
    const _createProgram = ts_impl.createProgram
    ts_impl.createProgram = function createProgram(rootNamesOrOptions: any): ts.Program {
      let oldProgram: ts.Program
      let compilerOptions: ts.CompilerOptions
      let compilerHost: ts.CompilerHost
      if ((<any>ts_impl).isArray(rootNamesOrOptions)) {
        compilerOptions = arguments[1]
        compilerHost = arguments[2]
        oldProgram = arguments[3]
      } else {
        const options = <ts.CreateProgramOptions>rootNamesOrOptions
        compilerOptions = options.options
        compilerHost = options.host
        oldProgram = options.oldProgram
      }
      if (compilerHost !== undefined && oldProgram !== undefined) {
        let scriptKindChanged: Set<string> = new Set()
        for (let sourceFile of oldProgram.getSourceFiles()) {
          if (sourceFile.fileName.endsWith(".mpx")) {
            try {
              // Check if we can safely acquire source code
              compilerHost.getSourceFileByPath(sourceFile.fileName, (<any>sourceFile).resolvedPath, compilerOptions.target,
                undefined, false)
            } catch (e) {
              // TODO - maybe we could change script kind here to avoid leak below
              scriptKindChanged.add((<any>sourceFile).resolvedPath)
              scriptKindChanged.add(sourceFile.fileName)
            }
          }
        }
        // Do not reuse old program structure if any of Mpx scripts have changed it's script kind
        if (scriptKindChanged.size > 0) {
          // TODO - forcing shouldCreateNewSourceFile is causing leaks in the document registry,
          //        as documents with changed script kind are acquired again

          // Patch compiler host to not fall into fail condition
          const _getSourceFileByPath = compilerHost.getSourceFileByPath
          compilerHost.getSourceFileByPath = function (fileName: string, path: ts.Path, languageVersion: ts.ScriptTarget,
                                                       onError?: (message: string) => void, shouldCreateNewSourceFile?: boolean): ts.SourceFile | undefined {
            // shouldCreateNewSourceFile
            arguments[4] = arguments[4] || scriptKindChanged.has(path) || scriptKindChanged.has(fileName)
            return _getSourceFileByPath.apply(compilerHost, arguments)
          }

          // Patch compiler host to not fall into fail condition
          const _getSourceFile = compilerHost.getSourceFile
          compilerHost.getSourceFile = function (fileName, languageVersion, onError, shouldCreateNewSourceFile): ts.SourceFile | undefined {
            // shouldCreateNewSourceFile
            arguments[3] = arguments[3] || scriptKindChanged.has(fileName)
            return _getSourceFile.apply(compilerHost, arguments)
          }

          // Drop old program
          if ((<any>ts_impl).isArray(rootNamesOrOptions)) {
            arguments[3] = undefined
          } else {
            rootNamesOrOptions.oldProgram = undefined
          }
        }
      }
      return _createProgram.apply(undefined, arguments)
    }

    // If script kind is changed for Mpx file, we need to create a new source file
    const _updateLanguageServiceSourceFile = ts_impl.updateLanguageServiceSourceFile
    ts_impl.updateLanguageServiceSourceFile = function (
      sourceFile: ts.SourceFile, scriptSnapshot: ts.IScriptSnapshot, version: string,
      textChangeRange: ts.TextChangeRange | undefined, aggressiveChecks?: boolean): ts.SourceFile {
      if (sourceFile.fileName.endsWith(".mpx")
        && (<any>scriptSnapshot).scriptKind !== undefined
        && (<any>scriptSnapshot).scriptKind !== (<any>sourceFile).scriptKind) {
        return ts_impl.createLanguageServiceSourceFile(sourceFile.fileName, scriptSnapshot,
          sourceFile.languageVersion, version, /*setNodeParents*/ true, (<any>scriptSnapshot).scriptKind);
      }
      return _updateLanguageServiceSourceFile.apply(undefined, arguments)
    }

  }

  function myLoadWithLocalCache<T>(names: string[], containingFile: string, redirectedReference: object | undefined, loader: (name: string, containingFile: string, redirectedReference: object | undefined) => T): T[] {
    if (names.length === 0) {
      return [];
    }
    const resolutions: T[] = [];
    const cache = new Map<string, T>();
    for (const name of names) {
      let result: T;
      if (cache.has(name)) {
        result = cache.get(name)!;
      }
      else {
        cache.set(name, result = loader(name, containingFile, redirectedReference));
      }
      resolutions.push(result);
    }
    return resolutions;
  }

  return {
    create(info: ts.server.PluginCreateInfo): ts.LanguageService {

      const pluginInfo = info;
      const tsLs = info.languageService;
      const tsLsHost = info.languageServiceHost;
      const project = info.project;
      const compilerHost = <ts.CompilerHost><any>project
      const getCanonicalFileName = ts_impl.sys.useCaseSensitiveFileNames ? toFileNameLowerCase : identity;

      // Allow resolve into Mpx files
      const mpx_sys = {
        ...ts_impl.sys,
        fileExists(path: string): boolean {
          if (path.endsWith(".mpx.ts")
            && ts_impl.sys.fileExists(path.substring(0, path.length - 3))) {
            return true
          }
          return ts_impl.sys.fileExists(path)
        }
      }
      let moduleResolutionCache = ts_impl.createModuleResolutionCache(project.getCurrentDirectory(),
        (x: any) => (compilerHost.getCanonicalFileName ?? getCanonicalFileName)(x), project.getCompilerOptions());
      const loader = (moduleName: string, containingFile: string, redirectedReference: any) => {
        const tsResolvedModule = ts_impl.resolveModuleName(moduleName, containingFile, project.getCompilerOptions(),
          mpx_sys, moduleResolutionCache, redirectedReference).resolvedModule!;
        if (tsResolvedModule && tsResolvedModule.resolvedFileName.endsWith('.mpx.ts')) {
          const resolvedFileName = tsResolvedModule.resolvedFileName.slice(0, -3);
          return {
            resolvedFileName: resolvedFileName,
            extension: ts_impl.Extension.Ts
          }
        }
        return tsResolvedModule
      }
      tsLsHost.resolveModuleNames = (moduleNames: any, containingFile: any, _reusedNames: any, redirectedReference: any) =>
        ((<any>ts_impl).loadWithLocalCache || myLoadWithLocalCache)(moduleNames, containingFile, redirectedReference, loader);

      // Strip non-TS content from the script
      const _getScriptSnapshot = tsLsHost.getScriptSnapshot;
      const mpxScriptCache = new MpxScriptCache(ts_impl,
        fileName => _getScriptSnapshot.call(tsLsHost, fileName),
        fileName => tsLsHost.getScriptVersion(fileName))
      tsLsHost.getScriptSnapshot = function (fileName): ts.IScriptSnapshot {
        if (fileName.endsWith(".mpx")) {
          return mpxScriptCache.getScriptSnapshot(fileName)
        }
        return _getScriptSnapshot.call(this, fileName);
      }

      // Provide script kind based on `lang` attribute
      const _getScriptKind = tsLsHost.getScriptKind
      tsLsHost.getScriptKind = function (fileName): ts.ScriptKind {
        if (fileName.endsWith(".mpx")) {
          return mpxScriptCache.getScriptKind(fileName)
        }
        return _getScriptKind.call(this, fileName)
      }

      return tsLs;
    }
  };
};

/* Copied from TS compiler/core.ts */

function identity<T>(x: T) { return x; }
function toLowerCase(x: string) { return x.toLowerCase(); }
const fileNameLowerCaseRegExp = /[^\u0130\u0131\u00DFa-z0-9\\/:\-_. ]+/g;
function toFileNameLowerCase(x: string) {
  return fileNameLowerCaseRegExp.test(x) ?
    x.replace(fileNameLowerCaseRegExp, toLowerCase) :
    x;
}