package com.chow.pomegranate.ui.component

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.chow.pomegranate.repository.currentLocalDate
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/** 回到前台立即校准，持续打开时也能跨日更新；后台不运行定时器。 */
@Composable
internal fun rememberCurrentDate(): LocalDate {
    val owner = LocalLifecycleOwner.current
    val date by produceState(currentLocalDate(), owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = currentLocalDate()
                delay(30_000)
            }
        }
    }
    return date
}
