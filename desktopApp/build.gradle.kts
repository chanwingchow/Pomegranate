import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipFile

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

// Compose Desktop does not automatically import JVM dependencies' consumer rules.
val collectDesktopConsumerRules by tasks.registering {
    val runtimeJars = configurations.named("runtimeClasspath")
    val rulesFile = layout.buildDirectory.file("proguard/consumer-rules.pro")
    inputs.files(runtimeJars)
    outputs.file(rulesFile)
    doLast {
        val rules = inputs.files.files
            .filter { it.isFile && it.extension == "jar" }
            .sortedBy { it.name }
            .flatMap { jar ->
                ZipFile(jar).use { zip ->
                    val consumerRules = zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("META-INF/proguard/") && it.name.endsWith(".pro") }
                        .sortedBy { it.name }
                        .map { entry ->
                            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                            "# ${jar.name}: ${entry.name}\n$content\n"
                        }.toList()
                    val className = Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
                    val serviceRules = zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
                        .sortedBy { it.name }
                        .flatMap { entry ->
                            val service = entry.name.removePrefix("META-INF/services/")
                            val providers = zip.getInputStream(entry).bufferedReader().use { reader ->
                                reader.readLines().map { it.substringBefore('#').trim() }
                                    .filter { className.matches(it) }
                            }
                            buildList {
                                add("# ${jar.name}: ${entry.name}")
                                if (className.matches(service)) add("-keepnames class $service")
                                providers.forEach { add("-keep class $it { public <init>(); }") }
                            }.asSequence()
                        }.toList()
                    consumerRules + serviceRules
                }
            }
        rulesFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(rules.joinToString("\n"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.chow.pomegranate.MainKt"

        buildTypes.release.proguard {
            obfuscate.set(true)
            optimize.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
            configurationFiles.from(collectDesktopConsumerRules)
        }

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
