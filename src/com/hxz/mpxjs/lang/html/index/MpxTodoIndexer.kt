// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.index

import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.psi.impl.cache.impl.BaseFilterLexerUtil
import com.intellij.psi.impl.cache.impl.todo.TodoIndexEntry
import com.intellij.psi.impl.cache.impl.todo.VersionedTodoIndexer
import com.intellij.util.indexing.FileContent
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxTodoIndexer : VersionedTodoIndexer() {
  override fun map(inputData: FileContent): Map<TodoIndexEntry, Int> {
    return BaseFilterLexerUtil.scanContent(inputData) { consumer ->
      MpxFilterLexer(consumer, SyntaxHighlighterFactory.getSyntaxHighlighter(
        MpxLanguage.INSTANCE, inputData.project, inputData.file).highlightingLexer)
    }.todoMap
  }

  override fun getVersion(): Int {
    return 1
  }
}
