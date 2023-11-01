package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.toasterofbread.db.Database

actual fun AppContext.getSqlDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(getFilesDir().resolve("spmp_database.db").absolutePath)
    Database.Schema.create(driver)
    return driver
}
