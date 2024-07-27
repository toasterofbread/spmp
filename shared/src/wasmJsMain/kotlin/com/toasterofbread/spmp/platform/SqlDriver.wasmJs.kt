package com.toasterofbread.spmp.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual fun AppContext.getSqlDriver(): SqlDriver =
    createDefaultWebWorkerDriver()
