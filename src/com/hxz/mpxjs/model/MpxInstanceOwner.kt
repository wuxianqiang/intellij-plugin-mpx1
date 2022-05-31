// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSRecordType
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.ecma6.TypeScriptInterface
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.types.*
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.codeInsight.resolveSymbolFromNodeModule
import com.hxz.mpxjs.index.MPX_MODULE
import com.hxz.mpxjs.model.source.MPX_NAMESPACE
import com.hxz.mpxjs.model.source.MpxContainerInfoProvider
import com.hxz.mpxjs.model.source.MpxSourceEntity
import com.hxz.mpxjs.types.MpxCompleteRecordType
import com.hxz.mpxjs.types.MpxComponentInstanceType
import com.hxz.mpxjs.types.MpxRefsType
import com.hxz.mpxjs.types.createStrictTypeSource
import java.util.*

interface MpxInstanceOwner : MpxScopeElement {
  val thisType: JSType
    get() = if (source != null && this is UserDataHolder) {
      CachedValuesManager.getManager(source!!.project).getCachedValue(this) {
        CachedValueProvider.Result.create(buildInstanceType(this), PsiModificationTracker.MODIFICATION_COUNT)
      }
    }
    else null ?: JSAnyType.get(source, false)
}

fun getDefaultMpxComponentInstanceType(context: PsiElement?): JSType? =
  resolveSymbolFromNodeModule(context, MPX_MODULE, "ComponentPublicInstance", TypeScriptTypeAlias::class.java)
    ?.typeDeclaration?.jsType
  ?: resolveSymbolFromNodeModule(context, MPX_MODULE, MPX_NAMESPACE, TypeScriptInterface::class.java)?.jsType

private val MPX_INSTANCE_PROPERTIES: List<String> = listOf(
  "\$el", "\$options", "\$parent", "\$root", "\$children", "\$refs", "\$slots",
  "\$scopedSlots", "\$isServer", "\$data", "\$props",
  "\$ssrContext", "\$vnode", "\$attrs", "\$listeners")

private val MPX_INSTANCE_METHODS: List<String> = listOf(
  "\$mount", "\$forceUpdate", "\$destroy", "\$set", "\$delete", "\$watch", "\$on",
  "\$once", "\$off", "\$emit", "\$nextTick", "\$createElement")

private fun buildInstanceType(instance: MpxInstanceOwner): JSType? {
  val source = instance.source ?: return null
  val result = mutableMapOf<String, JSRecordType.PropertySignature>()
  contributeDefaultInstanceProperties(source, result)
  contributeComponentProperties(instance, source, result)
  replaceStandardProperty("\$refs", MpxRefsType(createStrictTypeSource(source), instance), source, result)
  contributePropertiesFromProviders(instance, result)
  return MpxComponentInstanceType(JSTypeSourceFactory.createTypeSource(source, true), instance, result.values.toList())
}

private fun contributeDefaultInstanceProperties(source: PsiElement,
                                                result: MutableMap<String, JSRecordType.PropertySignature>): MutableMap<String, JSRecordType.PropertySignature> {
  val defaultInstanceType = getDefaultMpxComponentInstanceType(source)
  if (defaultInstanceType != null) {
    defaultInstanceType.asRecordType()
      .properties
      .associateByTo(result) { it.memberName }
  }
  else {
    // Fallback to a predefined list of properties without any typings
    MPX_INSTANCE_PROPERTIES.forEach {
      result[it] = createImplicitPropertySignature(it, null, source)
    }
    MPX_INSTANCE_METHODS.forEach {
      result[it] = createImplicitPropertySignature(it, null, source, kind = JSImplicitElement.Type.Function)
    }
  }
  return result
}

private fun contributePropertiesFromProviders(instance: MpxInstanceOwner, result: MutableMap<String, JSRecordType.PropertySignature>) {
  val unmodifiableResult = Collections.unmodifiableMap(result)
  MpxContainerInfoProvider.getProviders().asSequence()
    .flatMap { it.getThisTypeProperties(instance, unmodifiableResult).asSequence() }
    .associateByTo(result) { it.memberName }
}

private fun contributeComponentProperties(instance: MpxInstanceOwner,
                                          source: PsiElement,
                                          result: MutableMap<String, JSRecordType.PropertySignature>) {
  val proximityMap = mutableMapOf<String, MpxModelVisitor.Proximity>()

  val props = mutableMapOf<String, JSRecordType.PropertySignature>()
  val computed = mutableMapOf<String, JSRecordType.PropertySignature>()
  val data = mutableMapOf<String, JSRecordType.PropertySignature>()
  val methods = mutableMapOf<String, JSRecordType.PropertySignature>()

  instance.castSafelyTo<MpxEntitiesContainer>()
    ?.acceptPropertiesAndMethods(object : MpxModelProximityVisitor() {

      override fun visitInputProperty(prop: MpxInputProperty, proximity: Proximity): Boolean {
        process(prop, proximity, props, true)
        return true
      }

      override fun visitComputedProperty(computedProperty: MpxComputedProperty, proximity: Proximity): Boolean {
        process(computedProperty, proximity, computed, true)
        return true
      }

      override fun visitDataProperty(dataProperty: MpxDataProperty, proximity: Proximity): Boolean {
        process(dataProperty, proximity, data, false)
        return true
      }

      override fun visitMethod(method: MpxMethod, proximity: Proximity): Boolean {
        process(method, proximity, methods, false)
        return true
      }

      private fun process(property: MpxProperty,
                          proximity: Proximity,
                          dest: MutableMap<String, JSRecordType.PropertySignature>,
                          isReadOnly: Boolean) {
        if ((proximityMap.putIfAbsent(property.name, proximity) ?: proximity) >= proximity) {
          dest.merge(property.name,
                     JSRecordTypeImpl.PropertySignatureImpl(property.name, property.jsType, false, isReadOnly, property.source),
                     ::mergeSignatures)
        }
      }

    }, onlyPublic = false)

  replaceStandardProperty("\$props", props.values.toList(), source, result)
  replaceStandardProperty("\$data", data.values.toList(), source, result)
  replaceStandardProperty("\$options", buildOptionsType(instance, result["\$options"]?.jsType), source, result)

  // Mpx will not proxy data properties starting with _ or $
  // https://mpxjs.org/v2/api/#data
  // Interestingly it doesn't apply to computed, methods and props.
  data.keys.removeIf { it.startsWith("_") || it.startsWith("\$") }

  result.keys.removeIf {
    props.containsKey(it) || data.containsKey(it) || computed.containsKey(it) || methods.containsKey(it)
  }

  mergePut(result, props)
  mergePut(result, data)
  mergePut(result, computed)
  mergePut(result, methods)
}

private fun buildOptionsType(instance: MpxInstanceOwner, originalType: JSType?): JSType {
  val result = mutableListOf<JSType>()
  originalType?.let(result::add)
  instance.acceptEntities(object : MpxModelVisitor() {
    override fun visitMixin(mixin: MpxMixin, proximity: Proximity): Boolean = visitInstanceOwner(mixin)

    override fun visitSelfComponent(component: MpxComponent, proximity: Proximity): Boolean = visitInstanceOwner(component)

    override fun visitSelfApplication(application: MpxApp, proximity: Proximity): Boolean = visitInstanceOwner(application)

    fun visitInstanceOwner(instanceOwner: MpxInstanceOwner): Boolean {
      when (val initializer = (instanceOwner as? MpxSourceEntity)?.initializer) {
        is JSObjectLiteralExpression -> result.add(JSTypeofTypeImpl(
          initializer, JSTypeSourceFactory.createTypeSource(initializer, false)))
        is JSFile -> result.add(JSModuleTypeImpl(initializer, false))
      }
      return true
    }
  }, MpxModelVisitor.Proximity.LOCAL)
  return JSCompositeTypeFactory.createIntersectionType(
    result, originalType?.source ?: JSTypeSourceFactory.createTypeSource(instance.source!!, false))
}

private fun replaceStandardProperty(propName: String, properties: List<JSRecordType.PropertySignature>,
                                    defaultSource: PsiElement, result: MutableMap<String, JSRecordType.PropertySignature>) {
  val propSource = result[propName]?.memberSource?.singleElement ?: defaultSource
  result[propName] = createImplicitPropertySignature(
    propName, MpxCompleteRecordType(propSource, properties), propSource)
}

private fun replaceStandardProperty(propName: String, type: JSType,
                                    defaultSource: PsiElement, result: MutableMap<String, JSRecordType.PropertySignature>) {
  val propSource = result[propName]?.memberSource?.singleElement ?: defaultSource
  result[propName] = createImplicitPropertySignature(propName, type, propSource)
}

private fun mergeSignatures(existing: JSRecordType.PropertySignature,
                            updated: JSRecordType.PropertySignature): JSRecordType.PropertySignature {
  val existingType = existing.jsType
  val updatedType = updated.jsType
  val type: JSType? = if (existingType == null || updatedType == null)
    null
  else
    JSCompositeTypeFactory.createUnionType(existingType.source, existingType, updatedType)
  return JSRecordTypeImpl.PropertySignatureImpl(
    existing.memberName, type, existing.isOptional && updated.isOptional,
    false, JSRecordMemberSourceFactory.createSource(existing.memberSource.allSourceElements +
                                                    updated.memberSource.allSourceElements,
                                                    JSRecordType.MemberSourceKind.Union))
}

private fun mergePut(result: MutableMap<String, JSRecordType.PropertySignature>,
                     contributions: MutableMap<String, JSRecordType.PropertySignature>) =
  contributions.forEach { (name, value) ->
    result.merge(name, value, ::mergeSignatures)
  }

fun createImplicitPropertySignature(name: String,
                                    type: JSType?,
                                    source: PsiElement,
                                    equivalentToSource: Boolean = false,
                                    isReadOnly: Boolean = false,
                                    kind: JSImplicitElement.Type = JSImplicitElement.Type.Property): JSRecordType.PropertySignature {
  return JSRecordTypeImpl.PropertySignatureImpl(name, type, false, isReadOnly,
                                                MpxImplicitElement(name, type, source, kind, equivalentToSource))
}