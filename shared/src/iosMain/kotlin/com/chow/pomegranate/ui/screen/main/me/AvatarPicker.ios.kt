package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/** iOS 端复制相册图片到应用沙盒，避免保存临时相册引用。 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarPickResult) -> Unit): () -> Unit {
    val delegate = remember(onResult) { AvatarPickerDelegate(onResult) }
    return remember(delegate) { { presentAvatarPicker(delegate) } }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun deleteLocalAvatar(uri: String) {
    runCatching {
        val path = uri.removePrefix("file://")
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class AvatarPickerDelegate(
    private val onResult: (AvatarPickResult) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onResult(image?.saveAvatar() ?: AvatarPickResult.Failed("无法读取所选图片。"))
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentAvatarPicker(delegate: AvatarPickerDelegate) {
    val picker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        this.delegate = delegate
    }
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.saveAvatar(): AvatarPickResult = runCatching {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("无法定位应用数据目录。")
    val avatarDirectory = "$directory/avatars"
    NSFileManager.defaultManager.createDirectoryAtPath(
        avatarDirectory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    // 每次选择都写入新文件，避免复用 URI 时命中旧图片缓存
    val encoded = encodeAvatar()
    val path = "$avatarDirectory/${newLocalAvatarFileName(encoded.extension)}"
    check(NSFileManager.defaultManager.createFileAtPath(path, contents = encoded.data, attributes = null)) {
        "头像保存失败。"
    }
    AvatarPickResult.Selected("file://$path")
}.getOrElse { AvatarPickResult.Failed(it.message ?: "头像保存失败，请重试。") }

/**
 * 按是否含透明通道选择编码格式。
 *
 * 有透明像素时用 PNG，避免 JPEG 把透明区域压成黑块；其余情况用 JPEG 控制体积。
 * 无论如何都重新编码，因为相册取回的 `UIImage` 没有可直接落盘的原始文件。
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.encodeAvatar(): EncodedAvatar {
    val hasAlpha = CGImage?.let { image ->
        when (CGImageGetAlphaInfo(image)) {
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast,
            CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst,
            CGImageAlphaInfo.kCGImageAlphaLast,
            CGImageAlphaInfo.kCGImageAlphaFirst,
            CGImageAlphaInfo.kCGImageAlphaOnly,
            -> true

            else -> false
        }
    } ?: false

    if (hasAlpha) {
        val data = UIImagePNGRepresentation(this) ?: error("无法编码所选图片。")
        return EncodedAvatar(data, "png")
    }

    val data = UIImageJPEGRepresentation(this, compressionQuality = 0.9) ?: error("无法编码所选图片。")
    return EncodedAvatar(data, "jpg")
}

/** 编码后的头像数据和对应扩展名。 */
private class EncodedAvatar(
    val data: NSData,
    val extension: String,
)
