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
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterProperty
import com.toasterofbread.spmp.model.mediaitem.db.AltSetterSingleProperty
import com.toasterofbread.spmp.model.mediaitem.db.ListProperty
import com.toasterofbread.spmp.model.mediaitem.db.ListPropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.Property
import com.toasterofbread.spmp.model.mediaitem.db.PropertyImpl
import com.toasterofbread.spmp.model.mediaitem.db.SingleProperty
import mediaitem.song.ArtistById

open class PropertyRememberer {
    private val properties: MutableMap<String, Any> = mutableMapOf()

    protected open fun onRead(key: String) {}
    protected open fun onWrite(key: String) {}

    open inner class LocalProperty<T>(
        val key: String,
        val getValue: () -> T,
        val setValue: (T) -> Unit
    ): Property<T> {
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

    fun <T> rememberLocalSingleProperty(
        key: String,
        getValue: () -> T,
        setValue: (T) -> Unit
    ): Property<T> {
        return properties.getOrPut(key) {
            LocalProperty(key, getValue, setValue)
        } as Property<T>
    }

    fun <T: A, A> rememberAltSetterLocalSingleProperty(
        key: String,
        getValue: () -> T,
        setValue: (T) -> Unit,
        setValueAlt: (A) -> Unit
    ): AltSetterProperty<T, A> {
        return properties.getOrPut(key) {
            object : LocalProperty<T>(key, getValue, setValue), AltSetterProperty<T, A> {
                override fun setAlt(value: A, db: Database) {
                    onWrite(key)
                    setValueAlt(value)
                }
            }
        } as AltSetterProperty<T, A>
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

    fun <T: A, A, Q : Any> rememberAltSetterSingleQueryProperty(
        key: String,
        getQuery: Database.() -> Query<Q>,
        getValue: Q.() -> T,
        setValue: Database.(T) -> Unit,
        setValueAlt: Database.(A) -> Unit,
        getDefault: () -> T = { null as T }
    ): AltSetterProperty<T, A> {
        return properties.getOrPut(key) {
            AltSetterSingleProperty(
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
                setValueAlt = {
                    onWrite(key)
                    setValueAlt(it)
                },
                getDefault
            )
        } as AltSetterProperty<T, A>
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
