package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver

// SqlDelight migration is buggy and inconsistent between platforms, this hopefully won't be
object Migration {
    private val DATABASE_VERSION: Int = 9

    fun updateDriverIfNeeded(driver: SqlDriver) {
        val driver_version: Int = driver.getVersion()
        if (driver_version < DATABASE_VERSION) {
            println("Migrating database from version $driver_version to version $DATABASE_VERSION")

            for (version in driver_version until DATABASE_VERSION) {
                when (version) {
                    0 -> driver.migrateToVersion1()
                    1 -> driver.migrateToVersion2()
                    2, 3 -> {}
                    4 -> driver.migrateToVersion5()
                    5 -> driver.migrateToVersion6()
                    6 -> driver.migrateToVersion7()
                    7 -> driver.migrateToVersion8()
                    8 -> driver.migrateToVersion9()
                    else -> throw NotImplementedError(version.toString())
                }
            }

            driver.setVersion(DATABASE_VERSION)
        }
        else {
            println("Database is already up to date (version $driver_version)")
        }
    }

    interface Operation {
        fun execute(driver: SqlDriver, table: String)
    }

    data class AddColumn(val column: String, val type: String): Operation {
        override fun execute(driver: SqlDriver, table: String) {
            if (!driver.doesColumnExist(table, column)) {
                val query: String = """ALTER TABLE $table ADD COLUMN $column $type"""
                driver.execute(null, query, 0, null).value
            }
        }
    }

    data class CreateTable(val schema: String): Operation {
        override fun execute(driver: SqlDriver, table: String) {
            val query: String = """CREATE TABLE IF NOT EXISTS $table ($schema)"""
            driver.execute(null, query, 0, null).value
        }
    }

    internal fun SqlDriver.performMigration(operations: Map<String, List<Operation>>) {
        for (table in operations) {
            for (operation in table.value) {
                operation.execute(this, table.key)
            }
        }
    }
}

private fun SqlDriver.doesColumnExist(table: String, column: String): Boolean {
    val mapper: (SqlCursor) -> QueryResult.Value<Long?> =
        { cursor: SqlCursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
        }

    val query: String =
        """
        select count(*) from
        pragma_table_info("$table")
        where name="$column";
        """.trimIndent()

    return executeQuery(null, query, mapper, 0, null).value != 0L
}

private fun SqlDriver.getVersion(): Int {
    if (!doesColumnExist("Version", "version")) {
        run(
            """
            CREATE TABLE Version (
                version INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent()
        )
        run("INSERT INTO Version (version) VALUES (0);")
    }

    val mapper: (SqlCursor) -> QueryResult.Value<Long?> = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
    }
    return executeQuery(null, "SELECT version FROM Version", mapper, 0, null).value?.toInt() ?: 0
}

private fun SqlDriver.setVersion(version: Int) {
    run("UPDATE Version SET version = $version")
}

private fun SqlDriver.run(query: String) =
    execute(null, query, 0, null)
