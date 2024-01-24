package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.resources.migrations.Migration

expect fun AppContext.getSqlDriver(): SqlDriver

fun AppContext.createDatabase(): Database {
    val driver: SqlDriver = getSqlDriver()
    Migration.updateDriverIfNeeded(driver)
    return Database(driver)
}
