package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.login
import com.chow.pomegranate.login_required
import org.jetbrains.compose.resources.stringResource

/**
 * 未登录提示。
 *
 * 需要登录的页面在未登录时用它替代内容，避免请求受保护接口。
 *
 * @param onLogin 前往登录
 */
@Composable
fun NotLoggedInContent(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 提示
        Text(
            stringResource(Res.string.login_required),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // 前往登录
        Button(onClick = onLogin) {
            Text(stringResource(Res.string.login))
        }
    }
}
