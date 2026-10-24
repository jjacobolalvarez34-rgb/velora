import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Solo las anotaciones (@Immutable/@Stable), sin el runtime de Compose ni Android.
    api(platform(libs.compose.bom))
    api(libs.compose.runtime.annotation)

    testImplementation(libs.junit)
}
