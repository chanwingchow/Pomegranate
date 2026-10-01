import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.kotlinx.coroutinesSwing)

        implementation(projects.shared)
    }
}

compose.desktop {
    application {
        mainClass = "com.chow.pomegranate.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.chow.pomegranate"
            packageVersion = providers.environmentVariable("PACKAGE_VERSION").orNull
                ?: libs.versions.versionName.get()
            // Include modules used through reflection by OCR and database dependencies.
            includeAllModules = true

            macOS {
                iconFile.set(project.file("src/main/resources/icons/Pomegranate.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/Pomegranate.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icons/Pomegranate.png"))
            }
        }
    }
}
