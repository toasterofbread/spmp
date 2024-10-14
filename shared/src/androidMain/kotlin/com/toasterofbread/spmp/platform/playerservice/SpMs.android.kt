package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.platform.AppContext

actual fun getSpMsMachineId(context: AppContext): String {
    return getSpMsMachineIdFromFile(context.getFilesDir()!!.resolve("spmp_machine_id.txt"))
}
