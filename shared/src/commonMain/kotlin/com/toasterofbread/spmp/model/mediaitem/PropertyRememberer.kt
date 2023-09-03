package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty

open class PropertyRememberer {
    private val properties: MutableMap<String, Any> = mutableMapOf()

    protected open fun onRead(key: String) {}
    protected open fun onWrite(key: String) {}

    fun <T> rememberLocalSingleProperty(
        key: String,
        getValue: () -> T,
        setValue: (T) -> Unit
    ): Property<T> {
        return properties.getOrPut(key) {
            object : Property<T> {
                override fun get(db: Database): T {
                    onRead(key)
                    return getValue()
                }

                @Composable
                override fun observe(db: Database): MutableState<T> {
                    val state: MutableState<T> = remember(this) {
                        onRead(key)
                        mutableStateOf(getValue())
                    }
                    var launched: Boolean by remember(this) { mutableStateOf(false) }

                    LaunchedEffect(this, state.value) {
                        if (!launched) {
                            launched = true
                            return@LaunchedEffect
                        }

                        onWrite(key)
                        setValue(state.value)
                    }

                    return state
                }

                override fun set(value: T, db: Database) {
                    onWrite(key)
                    setValue(value)
                }
            }
        } as Property<T>
    }

    fun <T> rememberLocalListProperty(
        key: String,
        getValue: () -> List<T>
    ): ListProperty<T> {
        return properties.getOrPut(key) {
            object : ListProperty<T> {
                override fun get(db: Database): List<T>? {
                    onRead(key)
                    return getValue()
                }

                @Composable
                override fun observe(db: Database): State<List<T>?> {
                    onRead(key)
                    return remember { mutableStateOf(getValue()) }
                }

                override fun removeItem(index: Int, db: Database) {
                    onWrite(key)
                    throw NotImplementedError(key)
                }

                override fun moveItem(from: Int, to: Int, db: Database) {
                    onWrite(key)
                    throw NotImplementedError(key)
                }

                override fun addItem(item: T, index: Int?, db: Database) {
                    onWrite(key)
                    throw NotImplementedError(key)
                }

                override fun overwriteItems(items: List<T>, db: Database) {
                    onWrite(key)
                    throw NotImplementedError(key)
                }
            }
        } as ListProperty<T>
    }

    fun <T, Q : Any> rememberSingleQueryProperty(
        key: String,
        getQuery: Database.() -> Query<Q>,
        getValue: Q.() -> T,
        setValue: Database.(T) -> Unit,
        getDefault: () -> T = { null as T }
    ): Property<T> {
        return properties.getOrPut(key) {
            SingleProperty(
                getQuery = {
                    onRead(key)
                    getQuery()
                },
                getValue = {
                    onRead(key)
                    getValue()
                },
                setValue = {
                    onWrite(key)
                    setValue(it)
                },
                getDefault
            )
        } as Property<T>
    }

    fun <T, Q : Any> rememberListQueryProperty(
        key: String,
        getQuery: Database.() -> Query<Q>,
        getValue: List<Q>.() -> List<T>,
        getSize: Database.() -> Long,
        addItem: Database.(item: T, index: Long) -> Unit,
        removeItem: Database.(index: Long) -> Unit,
        setItemIndex: Database.(from: Long, to: Long) -> Unit,
        clearItems: Database.(from_index: Long) -> Unit,
        prerequisite: Property<Boolean>?,
    ): ListPropertyImpl<T, Q> {
        return properties.getOrPut(key) {
            ListPropertyImpl(
                getQuery = {
                    onRead(key)
                    getQuery()
                },
                getValue = {
                    onRead(key)
                    getValue(this)
                },
                getSize = {
                    onRead(key)
                    getSize()
                },
                addItem = { a, b ->
                    onWrite(key)
                    addItem(a, b)
                },
                removeItem = {
                    onWrite(key)
                    removeItem(it)
                },
                setItemIndex = { a, b ->
                    onWrite(key)
                    setItemIndex(a, b)
                },
                clearItems = {
                    onWrite(key)
                    clearItems(it)
                },
                prerequisite = prerequisite
            )
        } as ListPropertyImpl<T, Q>
    }
}
