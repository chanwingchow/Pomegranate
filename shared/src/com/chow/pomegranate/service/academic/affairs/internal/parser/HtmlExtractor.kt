package com.chow.pomegranate.service.academic.affairs.internal.parser

/**
 * 返回在 [html] 中截取的 <table id="[id]"></table>。
 */
internal fun extractTableHtml(html: String, id: String): String {
    val markerIndex = html.indexOf("id=\"$id\"")
    val tableCloseTag = "</table>"

    val tableOpen = html.lastIndexOf("<table", markerIndex)
    val tableClose = html.indexOf(tableCloseTag, markerIndex)

    return html.substring(tableOpen, tableClose + tableCloseTag.length)
}