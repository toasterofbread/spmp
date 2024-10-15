package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

internal fun SqlDriver.migrateToVersion8() = performMigration(
    mapOf(
        "PersistentQueueItem" to listOf(
            Migration.CreateTable(
                """
                    item_index INTEGER NOT NULL PRIMARY KEY,
                    id TEXT NOT NULL,

                    FOREIGN KEY (id) REFERENCES MediaItem(id)
                """.trimIndent()
            )
        ),
        "PersistentQueueMetadata" to listOf(
            Migration.CreateTable(
                """
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    queue_index INTEGER NOT NULL,
                    playback_position_ms INTEGER NOT NULL
                """.trimIndent()
            )
        )
    )
)
