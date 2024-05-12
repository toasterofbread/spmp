package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

internal fun SqlDriver.migrateToVersion6() = performMigration(
    mapOf(
        "Playlist" to listOf(
            Migration.AddColumn("owned_by_user", "INTEGER"),
            Migration.AddColumn("artists", "TEXT")
        ),
        "Song" to listOf(
            Migration.AddColumn("video_position", "INTEGER"),
            Migration.AddColumn("landscape_queue_opacity", "REAL"),
            Migration.AddColumn("artists", "TEXT")
        )
    )
)
