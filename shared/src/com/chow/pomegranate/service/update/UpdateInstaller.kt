package com.chow.pomegranate.service.update

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

internal interface UpdateInstaller {
    val target: UpdateTarget
    val directory: Path
    val openLabel: String

    suspend fun open(path: Path): String
}

@Composable
internal expect fun rememberUpdateInstaller(): UpdateInstaller?
