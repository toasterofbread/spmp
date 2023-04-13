package com.spectre7.spmp.platform

import com.spectre7.spmp.ProjectBuildConfig

@Suppress("KotlinConstantConditions")
fun isDebugBuild(): Boolean = ProjectBuildConfig.IS_DEBUG

fun getAppName(context: ProjectContext): String {
    val info = context.applicationInfo
    val string_id = info.labelRes
    return if (string_id == 0) info.nonLocalizedLabel.toString() else context.getString(string_id)
}