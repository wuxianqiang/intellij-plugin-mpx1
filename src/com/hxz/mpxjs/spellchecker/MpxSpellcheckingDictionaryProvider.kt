// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

class MpxSpellcheckingDictionaryProvider : BundledDictionaryProvider {
  override fun getBundledDictionaries(): Array<String> = arrayOf("mpx.dic")
}
