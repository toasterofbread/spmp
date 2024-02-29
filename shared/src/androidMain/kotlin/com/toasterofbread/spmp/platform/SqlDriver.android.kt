package com.toasterofbread.spmp.platform

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.toasterofbread.spmp.db.Database

actual fun AppContext.getSqlDriver(): SqlDriver =
    AndroidSqliteDriver(
        Database.Schema,
        ctx,
        "spmp_database.db",
        callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        }
    )
