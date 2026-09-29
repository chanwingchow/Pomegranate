package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.notice_search
import com.chow.pomegranate.search_clear
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource

/** 顶部导航里的搜索栏高度。 */
private val SearchBarHeight = 40.dp

/**
 * 顶部导航里的搜索栏。
 *
 * 容器自绘、无外边框，方便直接嵌入 [PomSearchTopBar] 的毛玻璃导航里；
 * 文字仍由 [BasicTextField] 承载，输入法、光标和选择行为与系统一致。
 *
 * @param query 当前关键词
 * @param onQueryChange 关键词变化
 * @param placeholder 占位文案
 * @param onSearch 键盘搜索键回调
 * @param modifier 修饰符
 */
@Composable
fun PomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSearch: (String) -> Unit = {},
) {
    val hint = placeholder ?: stringResource(Res.string.notice_search)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.height(SearchBarHeight).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 搜索图标
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 输入区
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = LocalTextStyle.current,
                        maxLines = 1,
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                )
            }

            // 清空
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.search_clear),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PomSearchBarPreview() {
    PomegranateExpressiveTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PomSearchBar(
                query = "教学通知",
                onQueryChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
