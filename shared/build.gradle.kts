import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.koinCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "com.chow.pomegranate.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    compilerOptions.freeCompilerArgs = listOf("-XXLanguage:+ExplicitBackingFields")

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        api(libs.compose.components.resources)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.tess4Android)
            // 扫码：CameraX 预览 + ML Kit 条码识别（模型内置，不联网下载）
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode)
        }
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.bundles.datetime)
            implementation(libs.bundles.network)
            implementation(libs.bundles.ui)
            implementation(libs.bundles.utils)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.tess4j)
            // 桌面端从图片解码二维码
            implementation(libs.zxing.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

/**
 * AGP 9 的 KMP 库插件把 Android 的 KSP 任务读取的处理器配置命名为
 * `kspAndroidMainKotlinProcessorClasspath`，而它在脚本求值后才创建，
 * 因此这里延迟挂载 Room 编译器。
 *
 * 若只挂到 `kspAndroidMain`，处理器不会进入任务的 classpath，
 * Room 便不会生成 AppDatabase_Impl，运行时会因 ClassNotFoundException 闪退。
 */
afterEvaluate {
    // AGP 9 的 KMP 库插件为 Android 的 KSP 任务创建了两个候选处理器配置，
    // 二者都挂上，确保处理器真正进入任务 classpath。
    listOf(
        "kspAndroidMainKotlinProcessorClasspath",
        "kspAndroidMainProcessorClasspath",
    ).forEach { name ->
        configurations.findByName(name)
            ?.dependencies
            ?.add(project.dependencies.create(libs.room.compiler.get()))
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "com.chow.pomegranate"
    publicResClass = true
}

buildkonfig {
    packageName = "com.chow.pomegranate"
    objectName = "BuildConfig"

    defaultConfigs {
        // 版本名称
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "versionName",
            value = libs.versions.versionName.get(),
        )
    }
}
