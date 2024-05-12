package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

internal fun SqlDriver.migrateToVersion2() = performMigration(
    mapOf(
        "Song" to listOf(
            Migration.AddColumn("loudness_db", "REAL"),
            Migration.AddColumn("explicit", "INTEGER"),
            Migration.AddColumn("background_image_opacity", "REAL"),
            Migration.AddColumn("image_shadow_radius", "REAL")
        ),
        "Artist" to listOf(
            Migration.AddColumn("shuffle_playlist_id", "TEXT")
        )
    )
)
