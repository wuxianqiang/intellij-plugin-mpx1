// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.impl

import com.intellij.psi.impl.source.xml.stub.XmlAttributeStub
import com.intellij.psi.stubs.*
import com.hxz.mpxjs.lang.html.psi.MpxRefAttribute

class MpxRefAttributeStubImpl : StubBase<MpxRefAttribute>, XmlAttributeStub<MpxRefAttribute> {

  val containingTagName: String
  val isList: Boolean
  val value: String

  constructor(parent: StubElement<*>?,
              dataStream: StubInputStream,
              elementType: IStubElementType<out MpxRefAttributeStubImpl, out MpxRefAttributeImpl>) : super(parent, elementType) {
    value = dataStream.readNameString() ?: ""
    isList = dataStream.readBoolean()
    containingTagName = dataStream.readNameString() ?: ""
  }

  constructor(psi: MpxRefAttributeImpl,
              parent: StubElement<*>?,
              elementType: IStubElementType<out MpxRefAttributeStubImpl, out MpxRefAttributeImpl>) : super(parent, elementType) {
    value = psi.value ?: ""
    isList = psi.isList
    containingTagName = psi.containingTagName
  }

  internal fun serialize(stream: StubOutputStream) {
    stream.writeName(value)
    stream.writeBoolean(isList)
    stream.writeName(containingTagName)
  }
}
