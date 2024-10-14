package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.resources.migrations.Migration

expect fun AppContext.getSqlDriver(): SqlDriver

private var instance: Database? = null

fun AppContext.createDatabase(): Database {
    if (instance == null) {
        val driver: SqlDriver = getSqlDriver()
        Migration.updateDriverIfNeeded(driver)
        instance = Database(driver)
    }
    return instance!!
}
