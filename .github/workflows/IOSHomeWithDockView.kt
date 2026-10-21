package com.example.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. نموذج بيانات تطبيقات الـ Dock
data class DockApp(
    val name: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

// 2. واجهة الشاشة الرئيسية
@Composable
fun IOSHomeWithDockView(modifier: Modifier = Modifier) {
    // قائمة التطبيقات داخل الـ Dock
    val dockApps = listOf(
        DockApp("الهاتف", Icons.Default.Call, listOf(Color.Green, Color(0xFF00E676))),
        DockApp("المتصفح", Icons.Default.Search, listOf(Color.Blue, Color(0xFF00B0FF))),
        DockApp("الرسائل", Icons.Default.Chat, listOf(Color(0xFF00C853), Color(0xFF69F0AE))),
        DockApp("الموسيقى", Icons.Default.MusicNote, listOf(Color.Pink, Color.Red))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3F51B5), Color(0xFF9C27B0), Color(0xFFFF9800))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // الساعة والتاريخ في أعلى الشاشة
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "11:13",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White
                )
                Text(
                    text = "Mercredi 21 octobre",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // الـ Dock السفلي
            IOSDockView(
                apps = dockApps,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
            )
        }
    }
}

// 3. مكون الـ Dock
@Composable
fun IOSDockView(apps: List<DockApp>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.25f)) // تأثير الزجاج الشفاف
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEach { app ->
            DockAppItem(app = app) {
                // إجراء النقر على التطبيق
                println("Ouverture de ${app.name}")
            }
        }
    }
}

// 4. أيقونة التطبيق مع تأثير الضغط (Animation)
@Composable
fun DockAppItem(app: DockApp, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        label = "button_scale"
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
        Icon(
            imageVector = app.icon,
            contentDescription = app.name,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
