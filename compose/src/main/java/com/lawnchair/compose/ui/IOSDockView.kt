package com.lawnchair.compose.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
importandroidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// نموذج بيانات تطبيقات الـ Dock
data class DockApp(
    val name: String,
    val iconName: String,
    val gradientColors: List<Color>
)

// مكون الـ Dock بتصميم iOS الزجاجي
@Composable
fun IOSDockView(
    apps: List<DockApp>,
    modifier: Modifier = Modifier,
    onAppClick: (DockApp) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.25f)) // تأثير الزجاج الشفاف (Frosted Glass)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                DockAppItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

// عنصر الأيقونة الفردية مع تأثير الضغط (Spring Animation)
@Composable
fun DockAppItem(app: DockApp, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        label = "dock_item_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(60.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(app.gradientColors))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White, shape = RoundedCornerShape(4.dp))
        )
    }
}
