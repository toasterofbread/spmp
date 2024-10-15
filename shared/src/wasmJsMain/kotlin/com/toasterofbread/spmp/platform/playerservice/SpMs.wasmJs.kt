package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext

actual fun getSpMsMachineId(context: AppContext): String =
    generateNewSpMsMachineId()
