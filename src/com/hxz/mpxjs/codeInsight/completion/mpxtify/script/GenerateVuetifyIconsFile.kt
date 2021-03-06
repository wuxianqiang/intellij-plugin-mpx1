// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.completion.mpxtify.script

import java.io.File
import java.io.PrintWriter

//Used for generating MpxtifyIcons.kt file
private object GenerateMpxtifyIconsFile {
  fun generateFile() {
    val writer = PrintWriter("MpxtifyIcons.kt", "UTF-8")
    writer.println("object MpxtifyIcons {\n" +
                   "\n" +
                   "  val materialAndFontAwesome = listOf(\n" +
                   "    //  https://github.com/google/material-design-icons/blob/master/iconfont/codepoints")
    File("materialIcons.txt").inputStream().bufferedReader().use {
      it.readLines().forEach { line ->
        writer.println("\"" + line.substringBefore(" ") + "\",")
      }
    }
    writer.println("//  https://github.com/FortAwesome/Font-Awesome/blob/master/svg-with-js/js/fontawesome-all.js")
    File("fontAwesome.txt").inputStream().bufferedReader().use {
      it.readLines().forEach { line ->
        writer.println("\"fa-" + line.substringBefore(":").substringAfter(" \"") + ",")
      }
    }
    writer.println(("  )\n" +
                    "}"))
    writer.close()
  }
}
