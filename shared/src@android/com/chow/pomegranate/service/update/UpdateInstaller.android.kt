package com.chow.pomegranate.service.update

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

@Composable
internal actual fun rememberUpdateInstaller(): UpdateInstaller? {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidUpdateInstaller(context) }
}

internal class UpdateFileProvider : FileProvider()

private class AndroidUpdateInstaller(private val context: Context) : UpdateInstaller {
    override val target = if ("arm64-v8a" in Build.SUPPORTED_ABIS) {
        UpdateTarget.AndroidArm64
    } else {
        UpdateTarget.AndroidOther
    }
    override val directory = Path(File(context.cacheDir, "app-updates").absolutePath)
    override val openLabel = "安装更新"

    override suspend fun open(path: Path): String {
        val manager = context.packageManager
        val file = File(path.toString())
        withContext(Dispatchers.IO) { validateApk(manager, file) }

        if (!manager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return "允许小石榴安装应用后，返回这里继续安装。"
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        @Suppress("DEPRECATION")
        val installer = manager.queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY or PackageManager.MATCH_DEFAULT_ONLY)
            .firstOrNull()?.activityInfo ?: throw UpdateFailure("未找到系统安装程序，请前往发布页。")
        intent.component = ComponentName(installer.packageName, installer.name)
        context.startActivity(intent)
        return "请在系统安装界面确认更新。"
    }

    @Suppress("DEPRECATION")
    private fun validateApk(manager: PackageManager, file: File) {
        val installed = manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val update = manager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: throw UpdateFailure("无法验证这个 Android 安装包。")
        if (update.packageName != context.packageName || update.longVersionCode <= installed.longVersionCode) {
            throw UpdateFailure("安装包与当前应用不匹配，或版本未高于当前安装版本。")
        }
        val signatures = installed.signingInfo?.apkContentsSigners.orEmpty().toSet()
        if (signatures.isEmpty() || update.signingInfo?.apkContentsSigners.orEmpty().toSet() != signatures) {
            throw UpdateFailure("安装包签名与当前应用不一致，无法覆盖安装。")
        }
    }
}

private const val APK_MIME = "application/vnd.android.package-archive"
