//buildscript {
//    ext {
//        compose_version = "1.5.0"
//    }
//}
//
//plugins {
//    id 'com.android.application' version '7.3.0' apply false
//    id 'com.android.library' version '7.3.0' apply false
//    id 'org.jetbrains.kotlin.android' version '1.7.10' apply false
//}
//
//tasks.register('clean', Delete) {
//    delete rootProject.buildDir
//}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform").apply(false)
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
}
