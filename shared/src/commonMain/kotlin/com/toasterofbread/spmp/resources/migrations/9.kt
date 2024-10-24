package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

internal fun SqlDriver.migrateToVersion9() = performMigration(
    mapOf(
        "AndroidWidget" to listOf(
            Migration.CreateTable(
                """
                    id INTEGER NOT NULL PRIMARY KEY,
                    configuration TEXT NOT NULL
                """.trimIndent()
            )
        )
    )
)
