package com.chow.pomegranate.pages

import androidx.compose.runtime.Composable
import com.chow.pomegranate.components.sections.Downloads
import com.chow.pomegranate.downloads.PROJECT_URL
import com.varabyte.kobweb.compose.dom.GenericTag
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.navigation.BasePath
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.Element

@Page
@Composable
fun HomePage() {
    Header(attrs = { classes("site-header") }) {
        Div(attrs = { classes("header-inner") }) {
            A(href = BasePath.prependTo("/"), attrs = { classes("brand") }) {
                Img(src = BasePath.prependTo("/assets/logo.png"), alt = "", attrs = {
                    attr("width", "46")
                    attr("height", "46")
                })
                Text("小石榴")
            }
            A(href = PROJECT_URL, attrs = { classes("source-link") }) {
                Text("GitHub")
            }
        }
    }

    Main(attrs = { classes("hero") }) {
        Div(attrs = { classes("hero-copy") }) {
            H1 { Text("小石榴") }
            P(attrs = { classes("subtitle") }) { Text("广财非官方教务客户端") }
            P(attrs = { classes("intro") }) { Text("查课表、看成绩、浏览教务通知。") }
            Downloads()
        }

        GenericTag<Element>("figure", attrs = { classes("preview") }) {
            Img(
                src = BasePath.prependTo("/assets/desktop-timetable.webp"),
                alt = "小石榴桌面端的周课表，演示数据",
                attrs = {
                    classes("preview-desktop")
                    attr("width", "800")
                    attr("height", "572")
                },
            )
            Img(
                src = BasePath.prependTo("/assets/phone-timetable.webp"),
                alt = "小石榴手机端的周课表，演示数据",
                attrs = {
                    classes("preview-phone")
                    attr("width", "540")
                    attr("height", "1200")
                },
            )
        }
    }
}
