package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import app.cash.sqldelight.Query
import com.toasterofbread.Database

interface Property<T> {
    fun get(db: Database): T
    fun set(value: T, db: Database)
    @Composable
    fun observe(db: Database): MutableState<T>
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
        getQuery(db).observeAsState(
            { getValue(it) },
            { setValue(db, it) }
        )
}

internal class SingleProperty<T, Q: Query<QueryValue>, QueryValue: Any>(
    getQuery: Database.() -> Q,
    getValue: QueryValue.() -> T,
    setValue: Database.(T) -> Unit
): PropertyImpl<T, Q>(
    getQuery, { getValue(executeAsOne()) }, setValue
)

class ListProperty<T, Q: Query<QueryValue>, QueryValue: Any>(
    private val getQuery: Database.() -> Q,
    private val getValue: List<QueryValue>.() -> List<T>,
    val getSize: Database.() -> Long,
    private val addItem: Database.(item: T, index: Long) -> Unit,
    private val removeItem: Database.(index: Long) -> Unit,
    private val setItemIndex: Database.(from: Long, to: Long) -> Unit,
    val clearItems: Database.(from_index: Long) -> Unit
) {
    fun get(db: Database): List<T> = getValue(getQuery(db).executeAsList())
    @Composable
    fun observe(db: Database): State<List<T>> =
        getQuery(db).observeAsState(
            { getValue(it.executeAsList()) },
            null
        )

    fun overwriteItems(items: List<T>, db: Database) {
        with(db) { transaction {
            clearItems(0)
            for (item in items.withIndex()) {
                addItem(item.value, item.index.toLong())
            }
        }}
    }

    fun addItem(item: T, index: Int, db: Database) {
        require(index >= 0)

        with(db) { transaction {
            val size = getSize(db)
            if (index > size) {
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

    fun removeItem(index: Int, db: Database) {
        require(index >= 0)

        with(db) { db.transaction {
            val size = getSize(db)
            removeItem(index.toLong())
            for (i in size - 1 downTo index + 1) {
                setItemIndex(i, i - 1)
            }
        }}
    }

    fun moveItem(from: Int, to: Int, db: Database) {
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
