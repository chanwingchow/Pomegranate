package com.chow.pomegranate.ui.screen.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.chow.pomegranate.ui.component.BackToTopButton
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.chow.pomegranate.ui.component.withoutBottom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.campus_canton
import com.chow.pomegranate.campus_foshan
import com.chow.pomegranate.contact_dial
import com.chow.pomegranate.contact_dial_failed
import com.chow.pomegranate.contact_hours
import com.chow.pomegranate.emergency_contact
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ExpressiveActionCard
import com.chow.pomegranate.ui.component.ExpressiveIconBadge
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

fun EntryProviderScope<NavKey>.contactEntry() {
    entry<ContactRoute> { ContactContent() }
}

@Serializable
data object ContactRoute : NavKey

@Preview(name = "手机", widthDp = 411, heightDp = 800)
@Preview(name = "窄屏", widthDp = 320, heightDp = 640)
@Preview(name = "宽屏", widthDp = 840, heightDp = 700)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContactContentPreview() {
    DynamicMaterialExpressiveTheme(state = rememberDynamicMaterialThemeState(primary = DEFAULT_PRIMARY_COLOR, isDark = false)) {
        CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }) { ContactContent() }
    }
}

/** 校区切换固定在顶部；每个号码有独立、明确的系统拨号入口。 */
@Composable
private fun ContactContent(modifier: Modifier = Modifier) {
    val backStack = LocalBackStack.current
    val uriHandler = LocalUriHandler.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val dialFailed = stringResource(Res.string.contact_dial_failed)
    val campuses = Campus.entries
    val pager = rememberPagerState { campuses.size }
    val grids = campuses.map { rememberLazyGridState() }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)

    Scaffold(
        modifier = modifier,
        topBar = {
            com.chow.pomegranate.ui.component.StudyTopBar {
                PomTopAppBar(
                    title = { Text(stringResource(Res.string.emergency_contact)) },
                    shadowElevation = 0.dp,
                    navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
                )
                com.chow.pomegranate.ui.component.StudyTabs(
                    campuses.map { stringResource(if (it == Campus.Canton) Res.string.campus_canton else Res.string.campus_foshan) }, pager.currentPage,
                ) { index -> scope.launch { pager.animateScrollToPage(index) } }

            }
        },
        floatingActionButton = { BackToTopButton(grids[pager.currentPage]) },
    ) { innerPadding ->
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(innerPadding.withoutBottom()), verticalAlignment = Alignment.Top) { page ->
            val contacts: List<EmergencyContact> = when (campuses[page]) {
                Campus.Canton -> EmergencyContact.Canton.entries
                Campus.Foshan -> EmergencyContact.Foshan.entries
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(320.dp * fontScale), state = grids[page],
                    modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + innerPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(contacts, key = { _, contact -> contact.phoneNumber }) { index, contact ->
                        ContactCard(contact, index) {
                            runCatching { uriHandler.openUri("tel:${contact.phoneNumber}") }
                                .onFailure { scope.launch { snackbar.showSnackbar(dialFailed) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(contact: EmergencyContact, index: Int, onDial: () -> Unit) {
    val department = stringResource(contact.department)
    val dialLabel = stringResource(Res.string.contact_dial, department)
    ExpressiveActionCard(onClick = onDial, modifier = Modifier.semantics { onClick(label = dialLabel, action = null) }) { pressed ->
        BoxWithConstraints(Modifier.padding(14.dp)) {
            val stackNumber = LocalDensity.current.fontScale > 1.3f || maxWidth < 280.dp
            val hours: @Composable () -> Unit = {
                contact.businessHours?.let { hours ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.contact_hours, "${hours.start}–${hours.endInclusive}"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExpressiveIconBadge(contact.icon, index, pressed)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(department, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!stackNumber) Text(contact.phoneNumber, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                        if (!stackNumber) hours()
                    }
                    FilledTonalIconButton(onClick = onDial, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Call, dialLabel, Modifier.size(20.dp))
                    }
                }
                if (stackNumber) {
                    Text(contact.phoneNumber, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    hours()
                }
            }
        }
    }
}
