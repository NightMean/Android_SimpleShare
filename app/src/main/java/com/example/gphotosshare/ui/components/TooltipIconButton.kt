package com.example.gphotosshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.delay

enum class TooltipPosition { Above, Below }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    tooltip: String,
    position: TooltipPosition = TooltipPosition.Below,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(contentAlignment = Alignment.Center) {
        // Use a Surface/Box that mimics IconButton but with combinedClickable
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Transparent, CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTooltip = true 
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        if (showTooltip) {
            val popupPositionProvider = remember(position) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                        val y = if (position == TooltipPosition.Above) {
                             anchorBounds.top - popupContentSize.height
                        } else {
                             anchorBounds.bottom
                        }
                        return IntOffset(x, y)
                    }
                }
            }

            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { showTooltip = false }
            ) {
                 Box(
                    modifier = Modifier
                        .padding(4.dp) // Spacing from anchor
                        .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = tooltip,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
             // Auto hide helper
            LaunchedEffect(showTooltip) {
                if (showTooltip) {
                    delay(1500)
                    showTooltip = false
                }
            }
        }
    }
}
