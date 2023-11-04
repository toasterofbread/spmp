package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.toasterofbread.db.Database
import java.io.File

actual fun AppContext.getSqlDriver(): SqlDriver {
    val database_file: File = getFilesDir().resolve("spmp_database.db")
    database_file.parentFile.mkdirs()

    val database_exists = database_file.exists()
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:" + database_file.absolutePath)

    if (!database_exists) {
        Database.Schema.create(driver)
    }

    return driver
}
