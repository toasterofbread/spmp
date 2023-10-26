package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.Database

expect fun AppContext.getSqlDriver(): SqlDriver
fun AppContext.createDatabase(): Database = Database(getSqlDriver())
