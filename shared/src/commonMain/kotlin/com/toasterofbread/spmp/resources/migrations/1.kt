package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

internal fun SqlDriver.migrateToVersion1() = performMigration(
    mapOf(
        "Playlist" to listOf(
            Migration.AddColumn("playlist_url", "TEXT")
        ),
        "SongFeedRow" to listOf(
            Migration.AddColumn("layout_type", "INTEGER")
        )
    )
)
