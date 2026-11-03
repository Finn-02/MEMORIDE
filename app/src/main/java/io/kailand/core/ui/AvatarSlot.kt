package io.kailand.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kailand.core.contract.AvatarPreview

typealias AvatarSlot = @Composable (AvatarPreview, String, Modifier) -> Unit
