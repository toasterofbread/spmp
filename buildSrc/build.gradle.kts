plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.gmazzo.buildconfig:plugin:5.3.5")
}

tasks.withType(JavaCompile::class) {
    options.release.set(21)
}
