plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.gmazzo.buildconfig:plugin:5.4.0")
}

tasks.withType(JavaCompile::class) {
    options.release.set(21)
}
