// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.html.HtmlStubBasedTagElementType
import com.intellij.psi.impl.source.xml.stub.XmlAttributeStubImpl
import com.intellij.psi.impl.source.xml.stub.XmlStubBasedAttributeElementType
import com.intellij.psi.impl.source.xml.stub.XmlStubBasedElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.xml.IXmlAttributeElementType
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.PathUtil
import com.hxz.mpxjs.index.MpxIdIndex
import com.hxz.mpxjs.index.MpxUrlIndex
import com.hxz.mpxjs.lang.html.MpxLanguage
import com.hxz.mpxjs.lang.html.psi.impl.MpxRefAttributeImpl
import com.hxz.mpxjs.lang.html.psi.impl.MpxRefAttributeStubImpl
import com.hxz.mpxjs.model.SLOT_TAG_NAME
import java.io.IOException

object MpxStubElementTypes {

  const val VERSION = 5

  val STUBBED_TAG = object : HtmlStubBasedTagElementType("MPX_STUBBED_TAG", MpxLanguage.INSTANCE) {
    override fun shouldCreateStub(node: ASTNode?): Boolean {
      return (node?.psi as? XmlTag)
               ?.let {
                 // top-level style/script/template tag
                 it.parentTag == null
                 // slot tag
                 || it.name == SLOT_TAG_NAME
                 // script tag with x-template
                 || (it.getAttributeValue("type") == "text/x-template"
                     && it.getAttribute("id") != null)
               } ?: false
    }
  }

  val SCRIPT_ID_ATTRIBUTE = object : XmlStubBasedAttributeElementType("MPX_SCRIPT_ID_ATTRIBUTE", MpxLanguage.INSTANCE) {
    override fun indexStub(stub: XmlAttributeStubImpl, sink: IndexSink) {
      stub.value?.let { sink.occurrence(MpxIdIndex.KEY, it) }
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
      return (node?.psi as? XmlAttribute)?.parent
        ?.getAttribute("type")?.value == "text/x-template"
    }
  }

  val SRC_ATTRIBUTE = object : XmlStubBasedAttributeElementType("MPX_SRC_ATTRIBUTE", MpxLanguage.INSTANCE) {
    override fun indexStub(stub: XmlAttributeStubImpl, sink: IndexSink) {
      stub.value
        ?.let { PathUtil.getFileName(it) }
        ?.takeIf { it.isNotBlank() }
        ?.let { sink.occurrence(MpxUrlIndex.KEY, it) }
    }
  }

  val MPX_STUBBED_ATTRIBUTE = object : XmlStubBasedAttributeElementType("MPX_STUBBED_ATTRIBUTE", MpxLanguage.INSTANCE) {
  }

  val REF_ATTRIBUTE: XmlStubBasedElementType<MpxRefAttributeStubImpl, MpxRefAttributeImpl> =
    object : XmlStubBasedElementType<MpxRefAttributeStubImpl, MpxRefAttributeImpl>("MPX_REF_ATTRIBUTE", MpxLanguage.INSTANCE),
             ICompositeElementType, IXmlAttributeElementType {

      @Throws(IOException::class)
      override fun serialize(stub: MpxRefAttributeStubImpl, dataStream: StubOutputStream) {
        stub.serialize(dataStream)
      }

      @Throws(IOException::class)
      override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): MpxRefAttributeStubImpl {
        return MpxRefAttributeStubImpl(parentStub, dataStream, this)
      }

      override fun createPsi(stub: MpxRefAttributeStubImpl): MpxRefAttributeImpl {
        return MpxRefAttributeImpl(stub, this)
      }

      override fun createPsi(node: ASTNode): MpxRefAttributeImpl {
        return MpxRefAttributeImpl(node)
      }

      override fun createStub(psi: MpxRefAttributeImpl, parentStub: StubElement<*>): MpxRefAttributeStubImpl {
        return MpxRefAttributeStubImpl(psi, parentStub, this)
      }

    }


}
