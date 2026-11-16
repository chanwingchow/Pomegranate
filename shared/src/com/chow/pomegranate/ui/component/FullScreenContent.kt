package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.error
import com.chow.pomegranate.no_academic_data
import org.jetbrains.compose.resources.stringResource

/**
 * 页面级加载动画尺寸。
 *
 * Material 3 的加载指示器容器默认就是 48dp；显式写死是为了保证每个页面调用
 * [LoadingContent] 时大小完全一致，不会因为外部约束不同而变样。
 */
val LoadingContentIndicatorSize: Dp = 48.dp

/**
 * 行内加载动画尺寸，用于按钮、列表页脚等位置。
 */
val InlineLoadingIndicatorSize: Dp = 24.dp

/**
 * 统一的加载动画。
 *
 * 全屏与行内都走这里，避免各处 `LoadingIndicator()` 默认尺寸不一致。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = LoadingContentIndicatorSize,
) {
    LoadingIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * 加载中内容。
 */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    size: Dp = LoadingContentIndicatorSize,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        PomLoadingIndicator(size = size)
    }
}

/**
 * 加载失败内容。
 */
@Composable
fun ErrorContent(
    throwable: Throwable,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            // 图标
            Icon(
                Icons.TwoTone.ErrorOutline,
                contentDescription = stringResource(Res.string.error),
                modifier = Modifier.size(48.dp),
            )
        }

        item {
            // 错误信息
            Text(
                throwable.message ?: "$throwable",
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 空内容。
 *
 * @param message 自定义文案，`null` 表示使用默认的“暂无数据”
 */
@Composable
fun EmptyContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    message: String? = null,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message ?: stringResource(Res.string.no_academic_data),
            textAlign = TextAlign.Center,
        )
    }
}
