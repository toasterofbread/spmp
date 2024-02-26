package com.toasterofbread.spmp.resources.migrations

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.resources.migrations.Migration.performMigration

fun SqlDriver.migrateToVersion5() = performMigration(
    mapOf(
        "Song" to listOf(
            Migration.AddColumn("background_wave_speed", "REAL"),
            Migration.AddColumn("background_wave_opacity", "REAL")
        )
    )
)
