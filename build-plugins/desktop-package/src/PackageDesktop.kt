package com.chow.pomegranate.build

import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.CompilationArtifact
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import proguard.Configuration
import proguard.ConfigurationParser
import proguard.ProGuard
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipFile
import kotlin.io.path.*

/** Replaces Compose Desktop's release ProGuard and native-distribution tasks. */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun packageDesktop(
    @Input runtimeClasspath: Classpath,
    @Input appJar: CompilationArtifact,
    @Input moduleRoot: Path,
    @Input catalogFile: Path,
    @Output outputDirectory: Path,
) {
    val environment = System.getenv()
    val os = System.getProperty("os.name").lowercase()
    val windows = os.startsWith("windows")
    val mac = os.contains("mac")
    val format = environment["PACKAGE_FORMAT"] ?: environment["PACKAGE_EXTENSION"]
        ?: if (windows) "msi" else if (mac) "dmg" else "deb"
    require(format in setOf("msi", "dmg", "deb", "app-image")) { "Unsupported PACKAGE_FORMAT: $format" }
    val catalog = catalogFile.readText()
    val version = environment["PACKAGE_VERSION"]
        ?: Regex("(?m)^versionName\\s*=\\s*\"([^\"]+)\"").find(catalog)!!.groupValues[1]
    require(Regex("[0-9]+(\\.[0-9]+){1,2}").matches(version)) { "PACKAGE_VERSION must be numeric: $version" }
    // CI supplies a full JDK via setup-java. A bootstrap JRE cannot run jpackage.
    val jdk = sequenceOf(environment["JPACKAGE_JAVA_HOME"], environment["JAVA_HOME"], System.getProperty("java.home"))
        .filterNotNull().map(Path::of)
        .firstOrNull { it.resolve("bin/jpackage${if (windows) ".exe" else ""}").isRegularFile() }
        ?: error("Set JPACKAGE_JAVA_HOME or JAVA_HOME to a full JDK 21 containing jpackage and jmods")
    val input = outputDirectory.resolve("input").createDirectories()
    val sources = (listOf(appJar.artifact) + runtimeClasspath.resolvedFiles).distinct()
    require(sources.all { it.isRegularFile() && it.extension == "jar" }) { "Expected a JAR runtime classpath: $sources" }
    val outputs = sources.mapIndexed { index, jar ->
        input.resolve(if (index == 0) "pomegranate.jar" else "${index}-${jar.fileName}")
    }
    val consumerRules = outputDirectory.resolve("consumer-rules.pro")
    consumerRules.writeText(collectConsumerRules(sources))
    val rules = outputDirectory.resolve("release.pro")
    fun quote(path: Path) = "'${path.toAbsolutePath().toString().replace('\\', '/').replace("'", "\\'")}'"
    var ocrModel: ByteArray? = null
    rules.writeText(buildString {
        sources.zip(outputs).forEach { (source, target) ->
            val model = ZipFile(source.toFile()).use { zip ->
                zip.getEntry("tessdata/eng.traineddata")?.let { zip.getInputStream(it).use { input -> input.readBytes() } }
            }
            val duplicateModel = model != null && ocrModel != null
            if (duplicateModel) require(model.contentEquals(ocrModel!!)) { "Conflicting packaged OCR models" }
            if (model != null && ocrModel == null) ocrModel = model
            val modelFilter = if (duplicateModel) ",!tessdata/eng.traineddata" else ""
            // Leave multi-release selection to ProGuard's default, as Compose did.
            appendLine("-injars ${quote(source)}(!module-info.class,!META-INF/*.SF,!META-INF/*.RSA,!META-INF/*.DSA$modelFilter)")
            appendLine("-outjars ${quote(target)}")
        }
        jdk.resolve("jmods").listDirectoryEntries("*.jmod").sorted().forEach {
            appendLine("-libraryjars ${quote(it)}(!**.jar;!module-info.class)")
        }
        appendLine("-dontoptimize")
        appendLine("-include ${quote(moduleRoot.resolve("compose-desktop-rules.pro"))}")
        appendLine("-keep public class com.chow.pomegranate.MainKt { public static void main(java.lang.String[]); }")
        appendLine("-include ${quote(moduleRoot.resolve("proguard-rules.pro"))}")
        appendLine("-include ${quote(consumerRules)}")
        appendLine("-printmapping ${quote(outputDirectory.resolve("mapping.txt"))}")
    })
    val configuration = Configuration()
    ConfigurationParser(rules.toFile(), System.getProperties()).use { it.parse(configuration) }
    ProGuard(configuration).execute()
    // Keep per-library JARs, including native libraries, service descriptors and OCR data.
    val classPath = outputs.drop(1).filter { it.exists() }.joinToString(" ") { it.fileName.toString() }
    addManifest(outputs.first(), classPath)
    val destination = moduleRoot.resolve("build/release/desktop/${if (format == "app-image") "app" else format}")
    destination.toFile().deleteRecursively()
    destination.createDirectories()
    val icon = moduleRoot.resolve("src/main/resources/icons/Pomegranate.${if (windows) "ico" else if (mac) "icns" else "png"}")
    val command = mutableListOf(
        jdk.resolve("bin/jpackage${if (windows) ".exe" else ""}").toString(),
        "--type", format, "--name", "com.chow.pomegranate", "--app-version", version,
        "--input", input.toString(), "--main-jar", "pomegranate.jar",
        "--main-class", "com.chow.pomegranate.MainKt", "--dest", destination.toString(),
        "--icon", icon.toString(), "--add-modules", "ALL-MODULE-PATH",
    )
    if (mac) command += listOf("--mac-package-identifier", "com.chow.pomegranate")
    if (!mac && !windows && format == "deb") command += listOf("--linux-package-name", "com.chow.pomegranate")
    val process = ProcessBuilder(command).inheritIO().start()
    check(process.waitFor() == 0) { "jpackage failed for $format" }
    println("Desktop distribution: $destination")
}

private fun collectConsumerRules(jars: List<Path>): String = buildString {
    val className = Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
    jars.forEach { jar ->
        ZipFile(jar.toFile()).use { zip ->
            zip.entries().asSequence().filterNot { it.isDirectory }.sortedBy { it.name }.forEach { entry ->
                if (entry.name.startsWith("META-INF/proguard/") && entry.name.endsWith(".pro")) {
                    appendLine("# ${jar.fileName}: ${entry.name}")
                    appendLine(zip.getInputStream(entry).bufferedReader().use { it.readText() })
                } else if (entry.name.startsWith("META-INF/services/")) {
                    val service = entry.name.removePrefix("META-INF/services/")
                    if (className.matches(service)) appendLine("-keepnames class $service")
                    zip.getInputStream(entry).bufferedReader().use { it.readLines() }
                        .map { it.substringBefore('#').trim() }.filter { className.matches(it) }
                        .forEach { appendLine("-keep class $it { public <init>(); }") }
                }
            }
        }
    }
}

private fun addManifest(jar: Path, classPath: String) {
    val temporary = jar.resolveSibling("manifest-${jar.fileName}")
    JarFile(jar.toFile()).use { source ->
        val manifest = source.manifest ?: Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = "com.chow.pomegranate.MainKt"
        manifest.mainAttributes[Attributes.Name.CLASS_PATH] = classPath
        JarOutputStream(Files.newOutputStream(temporary), manifest).use { target ->
            source.entries().asSequence().filter { !it.name.equals("META-INF/MANIFEST.MF", true) }.forEach {
                target.putNextEntry(java.util.jar.JarEntry(it.name))
                source.getInputStream(it).use { input -> input.copyTo(target) }
                target.closeEntry()
            }
        }
    }
    Files.move(temporary, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
}
