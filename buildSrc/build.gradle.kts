plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.gmazzo.buildconfig:plugin:5.4.0")
    implementation("xmlpull:xmlpull:1.1.3.1")
    implementation("com.github.kobjects:kxml2:2.4.1")
}

tasks.withType(JavaCompile::class) {
    options.release.set(23)
}
