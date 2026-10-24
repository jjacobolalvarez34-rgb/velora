plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
}

// detekt y ktlint corren como CLI con su propio classpath (ver docs/DECISIONES.md, ADR-012).
val detektCli: Configuration = configurations.create("detektCli")
val ktlintCli: Configuration = configurations.create("ktlintCli")

dependencies {
    detektCli(libs.detekt.cli)
    ktlintCli(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val kotlinSourceDirs = listOf(
    "app",
    "core/model",
    "core/data",
    "core/designsystem",
    "baselineprofile",
    "macrobenchmark",
)
    .map { module -> "$module/src" }
    .filter { file(it).exists() }

val ktlintPatterns = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Análisis estático con detekt."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    val report = layout.buildDirectory.file("reports/detekt/detekt.html").get().asFile
    args(
        "--input", kotlinSourceDirs.joinToString(","),
        "--config", "config/detekt/detekt.yml",
        "--build-upon-default-config",
        "--excludes", "**/build/**",
        "--report", "html:$report",
    )
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Revisa el estilo de Kotlin con ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args(listOf("--relative") + ktlintPatterns)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Corrige el estilo de Kotlin con ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args(listOf("-F", "--relative") + ktlintPatterns)
}
