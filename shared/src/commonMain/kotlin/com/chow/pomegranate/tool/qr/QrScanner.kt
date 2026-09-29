package com.chow.pomegranate.tool.qr

import androidx.compose.runtime.Composable

/**
 * 二维码扫描界面。
 *
 * 各平台实现不同：Android 与 iOS 用相机实时识别，
 * 桌面端从图片识别（选择图片或粘贴剪贴板图片）。
 *
 * @param onResult 识别成功后回调二维码内容
 * @param onDismiss 取消时回调
 */
@Composable
expect fun QrScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
)
