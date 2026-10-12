package com.example.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role

/**
 * Modifier to prevent accidental double-taps (anti-slop skipping)
 * and provide tactile haptic feedback.
 */
fun Modifier.debouncedClickable(
    debounceTimeMs: Long = 450L,
    enabled: Boolean = true,
    role: Role? = null,
    enableHaptics: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val haptic = LocalHapticFeedback.current

    clickable(
        enabled = enabled,
        role = role,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple()
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTimeMs) {
            lastClickTime = currentTime
            if (enableHaptics) {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Exception) {}
            }
            onClick()
        }
    }
}
