// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.libraries.componentDecorator

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSRecordType.PropertySignature
import com.intellij.lang.javascript.psi.JSRecordType.TypeMember
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.ecma6.ES6Decorator
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeListOwner
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.psi.PsiElement
import com.hxz.mpxjs.codeInsight.*
import com.hxz.mpxjs.model.*
import com.hxz.mpxjs.model.source.MpxContainerInfoProvider
import com.hxz.mpxjs.model.source.MpxContainerInfoProvider.MpxContainerInfo
import java.util.*

class MpxDecoratedComponentInfoProvider : MpxContainerInfoProvider.MpxDecoratedContainerInfoProvider(::MpxDecoratedComponentInfo) {

  private class MpxDecoratedComponentInfo constructor(clazz: JSClass) : MpxContainerInfo {
    override val mixins: List<MpxMixin>
    override val extends: List<MpxMixin>
    override val data: List<MpxDataProperty>
    override val computed: List<MpxComputedProperty>
    override val methods: List<MpxMethod>
    override val emits: List<MpxEmitCall>
    override val props: List<MpxInputProperty>
    override val model: MpxModelDirectiveProperties?

    init {
      val mixins = mutableListOf<MpxMixin>()
      val extends = mutableListOf<MpxMixin>()
      val data = mutableListOf<MpxDataProperty>()
      val computed = mutableListOf<MpxComputedProperty>()
      val methods = mutableListOf<MpxMethod>()
      val emits = mutableListOf<MpxEmitCall>()
      val props = mutableListOf<MpxInputProperty>()
      var model: MpxModelDirectiveProperties? = null

      clazz.jsType
        .asRecordType()
        .typeMembers
        .forEach { member ->
          val decorator = findDecorator(member, DECS)
          when (decorator?.decoratorName) {
            PROP_DEC -> if (member is PropertySignature) {
              props.add(MpxDecoratedInputProperty(member.memberName, member, decorator, 0))
            }
            PROP_SYNC_DEC -> if (member is PropertySignature) {
              computed.add(MpxDecoratedComputedProperty(member.memberName, member, decorator, 1))
              getNameFromDecorator(decorator)?.let { name ->
                props.add(MpxDecoratedInputProperty(name, member, decorator, 1))
                emits.add(MpxDecoratedPropertyEmitCall("update:$name", member))
              }
            }
            MODEL_DEC -> if (member is PropertySignature && model === null) {
              val name = getNameFromDecorator(decorator)
              model = MpxModelDirectiveProperties(member.memberName,
                                                  name ?: MpxModelDirectiveProperties.DEFAULT_EVENT)
              props.add(MpxDecoratedInputProperty(member.memberName, member, decorator, 1))
            }
            EMIT_DEC -> if (member is PropertySignature) {
              if (member.memberSource.singleElement is JSFunction) {
                emits.add(MpxDecoratedPropertyEmitCall(getNameFromDecorator(decorator)
                                                       ?: fromAsset(member.memberName),
                                                       member))
                methods.add(MpxDecoratedPropertyMethod(member.memberName, member))
              }
            }
            else -> when (member) {
              is PropertySignature -> {
                val source = member.memberSource.singleElement
                if (source is JSFunction) {
                  if (source.isGetProperty || source.isSetProperty) {
                    computed.add(MpxDecoratedComputedProperty(member.memberName, member, null, 0))
                  }
                  else {
                    methods.add(MpxDecoratedPropertyMethod(member.memberName, member))
                  }
                }
                else if (source is JSAttributeListOwner) {
                  data.add(MpxDecoratedDataProperty(member))
                }
              }
            }
          }
        }

      clazz.extendsList?.members?.forEach { extendItem ->
        if (extendItem.referenceText == null) {
          val call = extendItem.expression as? JSCallExpression
          if ((call?.methodExpression as? JSReferenceExpression)?.referenceName?.toLowerCase(Locale.US) == "mixins") {
            call.arguments.mapNotNullTo(mixins) { arg ->
              (arg as? JSReferenceExpression)?.resolve()
                ?.let { MpxModelManager.getMixin(it) }
            }
          }
        }
        else {
          extendItem.classes.mapNotNullTo(mixins) {
            MpxModelManager.getMixin(it)
          }
        }
      }

      this.mixins = mixins
      this.extends = extends
      this.data = data
      this.computed = computed
      this.methods = methods
      this.emits = emits
      this.props = props
      this.model = model
    }

    companion object {

      private const val PROP_DEC = "Prop"
      private const val PROP_SYNC_DEC = "PropSync"
      private const val MODEL_DEC = "Model"
      private const val EMIT_DEC = "Emit"

      private val DECS = setOf(PROP_DEC, PROP_SYNC_DEC, MODEL_DEC, EMIT_DEC)

      private fun getNameFromDecorator(decorator: ES6Decorator): String? {
        return getDecoratorArgument(decorator, 0)
          ?.let { getTextIfLiteral(it) }
      }
    }

    private abstract class MpxDecoratedNamedSymbol<T : TypeMember>(override val name: String, protected val member: T)
      : MpxNamedSymbol {
      override val source: PsiElement? get() = member.memberSource.singleElement
    }

    private abstract class MpxDecoratedProperty(name: String, member: PropertySignature)
      : MpxDecoratedNamedSymbol<PropertySignature>(name, member), MpxProperty {
      override val jsType: JSType? get() = member.jsType
    }

    private class MpxDecoratedInputProperty(name: String, member: PropertySignature, decorator: ES6Decorator?, decoratorArgumentIndex: Int)
      : MpxDecoratedProperty(name, member), MpxInputProperty {

      override val jsType: JSType = MpxDecoratedComponentPropType(member, decorator, decoratorArgumentIndex)
      override val required: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        getRequiredFromPropOptions(getDecoratorArgument(decorator, decoratorArgumentIndex))
      }
    }

    private class MpxDecoratedComputedProperty(name: String,
                                               member: PropertySignature,
                                               decorator: ES6Decorator?,
                                               decoratorArgumentIndex: Int)
      : MpxDecoratedProperty(name, member), MpxComputedProperty {

      override val jsType: JSType = MpxDecoratedComponentPropType(member, decorator, decoratorArgumentIndex)
      override val source: PsiElement = MpxImplicitElement(name, jsType,
                                                           member.memberSource.singleElement!!,
                                                           JSImplicitElement.Type.Property, false)
    }

    private class MpxDecoratedDataProperty(member: PropertySignature)
      : MpxDecoratedProperty(member.memberName, member), MpxDataProperty

    private class MpxDecoratedPropertyEmitCall(name: String, member: PropertySignature)
      : MpxDecoratedNamedSymbol<PropertySignature>(name, member), MpxEmitCall {
      override val eventJSType: JSType? get() = member.jsType
    }

    private class MpxDecoratedPropertyMethod(name: String, member: PropertySignature)
      : MpxDecoratedProperty(name, member), MpxMethod

  }


}
