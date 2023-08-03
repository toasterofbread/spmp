package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.cash.sqldelight.Query
import com.toasterofbread.Database

interface Property<T> {
    fun get(db: Database): T
    fun set(value: T, db: Database)
    @Composable
    fun observe(db: Database): MutableState<T>

    @Composable
    fun <V> observeOn(db: Database, getProperty: (T) -> Property<V>?): V? {
        val value: T by observe(db)
        val property: Property<V>? = remember(value) { getProperty(value) }
        return property?.observe(db)?.value
    }

    fun setNotNull(value: T, db: Database) {
        if (value != null) {
            set(value, db)
        }
    }
}

internal open class PropertyImpl<T, Q: Query<*>>(
    private val getQuery: Database.() -> Q,
    private val getValue: Q.() -> T,
    private val setValue: Database.(T) -> Unit
): Property<T> {
    override fun get(db: Database): T = getValue(getQuery(db))
    override fun set(value: T, db: Database) = setValue(db, value)

    @Composable
    override fun observe(db: Database): MutableState<T> =
        remember(this) { getQuery(db) }.observeAsState(
            { getValue(it) },
            { setValue(db, it) }
        )
}

internal open class SingleProperty<T, Q: Any>(
    getQuery: Database.() -> Query<Q>,
    getValue: Q.() -> T,
    setValue: Database.(T) -> Unit,
    getDefault: () -> T = { null as T }
): PropertyImpl<T, Query<Q>>(
    getQuery,
    {
        val query_result = executeAsOneOrNull()
        if (query_result == null) getDefault() else getValue(query_result)
    },
    setValue
)

open class ListProperty<T, Q: Any>(
    private val getQuery: Database.() -> Query<Q>,
    private val getValue: List<Q>.() -> List<T>,
    val getSize: Database.() -> Long,
    private val addItem: Database.(item: T, index: Long) -> Unit,
    private val removeItem: Database.(index: Long) -> Unit,
    private val setItemIndex: Database.(from: Long, to: Long) -> Unit,
    val clearItems: Database.(from_index: Long) -> Unit,
    private val prerequisite: Property<Boolean>? = null
) {
    open fun get(db: Database): List<T>? {
        if (prerequisite?.get(db) == false) {
            return null
        }
        return getValue(getQuery(db).executeAsList())
    }

    @Composable
    open fun observe(db: Database): State<List<T>?> {
        val value_state = getQuery(db).observeAsState(
            { getValue(it.executeAsList()) },
            null
        )

        if (prerequisite != null) {
            val pr: Boolean by prerequisite.observe(db)
            return remember {
                derivedStateOf {
                    if (!pr) null
                    else value_state.value
                }
            }
        }

        return value_state
    }

    open fun overwriteItems(items: List<T>, db: Database) {
        with(db) { transaction {
            clearItems(0)
            for (item in items.withIndex()) {
                addItem(item.value, item.index.toLong())
            }
        }}
    }

    open fun addItem(item: T, index: Int?, db: Database) {
        if (index != null) {
            require(index >= 0)
        }

        with(db) { transaction {
            val size = getSize(db)
            if (index == null || index > size) {
                addItem(item, size)
            }
            else {
                for (i in size - 1 downTo index) {
                    setItemIndex(i, i + 1)
                }
                addItem(item, index.toLong())
            }
        }}
    }

    open fun removeItem(index: Int, db: Database) {
        require(index >= 0)

        with(db) { db.transaction {
            val size = getSize(db)
            removeItem(index.toLong())
            for (i in size - 1 downTo index + 1) {
                setItemIndex(i, i - 1)
            }
        }}
    }

    open fun moveItem(from: Int, to: Int, db: Database) {
        if (from == to) {
            return
        }

        require(from >= 0)
        require(to >= 0)

        val from = from.toLong()
        val to = to.toLong()

        db.transaction {
            setItemIndex(db, from, to)
            if (to > from) {
                for (i in from until to) {
                    setItemIndex(db, i, i - 1)
                }
            }
            else {
                for (i in to + 1 .. from) {
                    setItemIndex(db, i, i + 1)
                }
            }
        }
    }
}

internal fun <T> T?.asMediaItemProperty(base_property: Property<T>, setValue: (T) -> Unit): Property<T> {
    if (this == null) {
        return base_property
    }

    return object : Property<T> {
        override fun get(db: Database): T = this@asMediaItemProperty
        @Composable
        override fun observe(db: Database): MutableState<T> = mutableStateOf(this@asMediaItemProperty)
        override fun set(value: T, db: Database) {
            base_property.set(value, db)
            setValue(value)
        }
    }
}
