package com.chow.pomegranate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform