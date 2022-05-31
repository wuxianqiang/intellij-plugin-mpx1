package com.hxz.mpxjs.intentions.expandVModel

import com.intellij.javascript.web.codeInsight.html.elements.WebSymbolElementDescriptor
import com.intellij.lang.javascript.intentions.JavaScriptIntention
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlElementType
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxDirectiveInfo
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxDirectiveKind
import com.hxz.mpxjs.context.isMpxContext
import com.hxz.mpxjs.web.getModel

class MpxExpandVModelIntention : JavaScriptIntention() {
  override fun getFamilyName(): String = MpxBundle.message("mpx.template.intention.v-model.expand.family.name")
  override fun getText(): String = this.familyName
  private val validModifiers = setOf("lazy", "number", "trim")

  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean =
    element.node.elementType == XmlElementType.XML_NAME
           && element.parent
             ?.let {
               it.node.elementType == XmlElementType.XML_ATTRIBUTE
               && it.isValid  && it is XmlAttribute
               && it.parent?.descriptor is WebSymbolElementDescriptor
               && isValidVModel(it)
             } == true
           && isMpxContext(element)

  private fun isValidVModel(attribute: XmlAttribute): Boolean {
    val info = MpxAttributeNameParser.parse((attribute.name), attribute.parent)
    return (info as? MpxDirectiveInfo)?.directiveKind == MpxDirectiveKind.MODEL
           && validModifiers.containsAll(info.modifiers)
  }

  override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
    editor ?: return
    val parent: PsiElement = psiElement.parent
    val modelAttribute = parent as XmlAttribute
    val componentTag = modelAttribute.parent
    val componentDescriptor = componentTag.descriptor as? WebSymbolElementDescriptor ?: return

    val model = componentDescriptor.getModel()
    var event = model.event
    val prop = model.prop
    val info = MpxAttributeNameParser.parse(parent.name, parent.parent)

    val modifiers = info.modifiers
    var eventValue = "\$event"
    if (modifiers.contains("trim")) {
      eventValue = "typeof $eventValue === 'string' ? $eventValue.trim() : $eventValue"
    }
    if (modifiers.contains("number")) {
      eventValue = "isNaN(parseFloat($eventValue)) ? $eventValue : parseFloat($eventValue)"
    }
    if (modifiers.contains("lazy")) {
      event = "change"
    }
    CommandProcessor.getInstance().executeCommand(project, {
      WriteAction.run<RuntimeException> {
        modelAttribute.name = ":$prop"
        componentTag.setAttribute("@$event", "${modelAttribute.value} = $eventValue")
      }
    }, MpxBundle.message("mpx.template.intention.v-model.expand.command.name"), "MpxExpandVModel")
  }
}
