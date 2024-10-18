package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object ProjectJson {
    val instance: Json =
        Json {
            useArrayPolymorphism = true
            serializersModule = SerializersModule {
                // I have no idea why, but these aren't detected automatically
                for (type in SpMpWidgetType.entries) {
                    registerSerialiser(type.clickActionClass)
                }
            }
        }

    @OptIn(InternalSerializationApi::class)
    private fun <T: TypeWidgetClickAction> SerializersModuleBuilder.registerSerialiser(subclass: KClass<T>) {
        polymorphic(TypeWidgetClickAction::class, subclass, subclass.serializer())
    }
}
