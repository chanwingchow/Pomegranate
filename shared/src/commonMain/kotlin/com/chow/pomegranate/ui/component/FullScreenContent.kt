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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.error
import org.jetbrains.compose.resources.stringResource

/**
 * 加载中内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
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