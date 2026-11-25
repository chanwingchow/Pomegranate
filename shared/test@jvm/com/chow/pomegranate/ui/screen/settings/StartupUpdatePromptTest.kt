package com.chow.pomegranate.ui.screen.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StartupUpdatePromptTest {
    @Test
    fun promptsByDefaultAndHonorsGlobalSetting() {
        assertTrue(shouldShowStartupUpdate("1.2.0", enabled = true, skipped = null, deferred = null, nowMillis = 100))
        assertFalse(shouldShowStartupUpdate("1.2.0", enabled = false, skipped = null, deferred = null, nowMillis = 100))
    }

    @Test
    fun skippingOnlySuppressesTheSelectedVersion() {
        assertFalse(shouldShowStartupUpdate("1.2.0", true, "1.1.0,1.2.0", null, 100))
        assertTrue(shouldShowStartupUpdate("1.3.0", true, "1.1.0,1.2.0", null, 100))
    }

    @Test
    fun deferringExpiresAndDoesNotAffectNewerVersions() {
        assertFalse(shouldShowStartupUpdate("1.2.0", true, null, "1.2.0:100", 99))
        assertTrue(shouldShowStartupUpdate("1.2.0", true, null, "1.2.0:100", 100))
        assertTrue(shouldShowStartupUpdate("1.3.0", true, null, "1.2.0:100", 99))
    }
}
