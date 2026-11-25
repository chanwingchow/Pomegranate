import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.link
import kotlinx.html.meta

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
}

group = "com.chow.pomegranate"
version = "1.0-SNAPSHOT"

kobweb {
    app {
        index {
            lang.set("zh-CN")
            description.set("小石榴是开源的广财非官方教务客户端。查课表、看成绩、浏览教务通知。")
            faviconPath.set("/assets/logo.png")
            head.add {
                link(rel = "stylesheet", href = basePath.prependTo("/site.css"))
                meta(name = "theme-color", content = "#ffffff")
            }
        }
    }
}

kotlin {
    configAsKobwebApplication("pomegranate")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
        }
    }
}
