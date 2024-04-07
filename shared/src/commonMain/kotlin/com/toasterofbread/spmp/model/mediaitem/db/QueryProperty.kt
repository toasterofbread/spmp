package com.toasterofbread.spmp.model.mediaitem.db

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState

@Composable
fun <T: MediaItem?> Property<T>.observePropertyActiveTitle(): State<String?>? {
    val player: PlayerState = LocalPlayerState.current
    val item: MediaItem? by observe(player.database)
    return item?.observeActiveTitle()
}

@Composable
fun <T: MediaItem> Property<List<T>?>.observePropertyActiveTitles(): List<String?>? {
    val player: PlayerState = LocalPlayerState.current
    val items: List<T>? by observe(player.database)
    return items?.map { it.observeActiveTitle().value }
}

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

    fun setUncertain(value: T, db: Database) {
        if (value == null) {
            return
        }

        db.transaction {
            if (get(db) == null) {
                set(value, db)
            }
        }
    }

    fun setNotNull(value: T, db: Database, uncertain: Boolean = false) {
        if (uncertain) {
            setUncertain(value, db)
        }
        else if (value != null) {
            set(value, db)
        }
    }
}

interface AltSetterProperty<T: I, I>: Property<T> {
    fun setAlt(value: I, db: Database)

    fun setUncertainAlt(value: I, db: Database) {
        if (value == null) {
            return
        }

        db.transaction {
            if (get(db) == null) {
                setAlt(value, db)
            }
        }
    }

    fun setNotNullAlt(value: I, db: Database, uncertain: Boolean = false) {
        if (uncertain) {
            setUncertainAlt(value, db)
        }
        else if (value != null) {
            setAlt(value, db)
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
    override fun observe(db: Database): MutableState<T> {
        return remember(this) { getQuery(db) }
            .observeAsState(
                key = this,
                mapValue = { getValue(it) },
                onExternalChange = {
                    setValue(db, it)
                }
            )
    }
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

internal open class AltSetterSingleProperty<T: A, A, Q: Any>(
    getQuery: Database.() -> Query<Q>,
    getValue: Q.() -> T,
    setValue: Database.(T) -> Unit,
    val setValueAlt: Database.(A) -> Unit,
    getDefault: () -> T = { null as T }
): PropertyImpl<T, Query<Q>>(
    getQuery,
    {
        val query_result = executeAsOneOrNull()
        if (query_result == null) getDefault() else getValue(query_result)
    },
    setValue
), AltSetterProperty<T, A> {
    override fun setAlt(value: A, db: Database) {
        setValueAlt(db, value)
    }
}


interface ListProperty<T> {
    fun get(db: Database): List<T>?

    @Composable
    fun observe(db: Database, key: Any): State<List<T>?>

    @Composable
    fun observe(db: Database): State<List<T>?> = observe(db, Unit)

    fun overwriteItems(items: List<T>, db: Database)

    fun addItem(item: T, index: Int?, db: Database)
    fun removeItem(index: Int, db: Database)
    fun moveItem(from: Int, to: Int, db: Database)
}

open class ListPropertyImpl<T, Q: Any>(
    private val getQuery: Database.() -> Query<Q>,
    private val getValue: List<Q>.() -> List<T>,
    val getSize: Database.() -> Long,
    private val addItem: Database.(item: T, index: Long) -> Unit,
    private val removeItem: Database.(index: Long) -> Unit,
    private val setItemIndex: Database.(from: Long, to: Long) -> Unit,
    val clearItems: Database.(from_index: Long) -> Unit,
    private val prerequisite: Property<Boolean>? = null
): ListProperty<T> {
    override fun get(db: Database): List<T>? {
        if (prerequisite?.get(db) == false) {
            return null
        }
        return getValue(getQuery(db).executeAsList())
    }

    @Composable
    override fun observe(db: Database, key: Any): State<List<T>?> {
        val value_state = getQuery(db).observeAsState(
            key,
            { getValue(it.executeAsList()) },
            null
        )

        if (prerequisite != null) {
            val prerequisite_state: State<Boolean> = prerequisite.observe(db)
            return derivedStateOf {
                if (!prerequisite_state.value) null
                else value_state.value
            }
        }

        return value_state
    }

    override fun overwriteItems(items: List<T>, db: Database) {
        with(db) { transaction {
            clearItems(0)
            for (item in items.withIndex()) {
                addItem(item.value, item.index.toLong())
            }
        }}
    }

    override fun addItem(item: T, index: Int?, db: Database) {
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

    override fun removeItem(index: Int, db: Database) {
        require(index >= 0)

        with(db) { db.transaction {
            val size: Long = getSize(db)
            removeItem(index.toLong())
            for (i in index + 1 until size) {
                setItemIndex(i, i - 1)
            }
        }}
    }

    override fun moveItem(from: Int, to: Int, db: Database) {
        if (from == to) {
            return
        }

        require(from >= 0)
        require(to >= 0)

        val from = from.toLong()
        val to = to.toLong()

        db.transaction {
            val size = getSize(db)
            setItemIndex(db, from, size)

            if (to > from) {
                for (i in from + 1 .. to) {
                    setItemIndex(db, i, i - 1)
                }
            }
            else {
                for (i in from - 1 downTo to) {
                    setItemIndex(db, i, i + 1)
                }
            }

            setItemIndex(db, size, to)
        }
    }
}
