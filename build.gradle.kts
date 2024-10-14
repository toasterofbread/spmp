plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform") apply false
    kotlin("plugin.compose") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
}

// TEMP
// https://github.com/cashapp/sqldelight/pull/4965
//subprojects {
//    configurations.all {
//        resolutionStrategy {
//            eachDependency {
//                if (requested.group.toString() == "app.cash.sqldelight") {
//                    val sqldelightVersion: String = extra["sqldelight.version"] as String
//                    useTarget("com.github.toasterofbread.sqldelight:${requested.name}:$sqldelightVersion")
//                }
//            }
//        }
//    }
//}
//
