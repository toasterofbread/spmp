package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import com.toasterofbread.Database

expect fun PlatformContext.getSqlDriver(): SqlDriver
fun PlatformContext.createDatabase(): Database = Database(getSqlDriver())
