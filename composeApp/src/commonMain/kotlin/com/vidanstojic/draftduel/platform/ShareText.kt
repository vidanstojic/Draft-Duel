package com.vidanstojic.draftduel.platform

import androidx.compose.runtime.Composable

/**
 * Opens the system share sheet with [text]. One of the few platform-specific pieces:
 * Android uses an ACTION_SEND chooser, iOS a UIActivityViewController.
 */
@Composable
expect fun rememberShareText(): (text: String) -> Unit
