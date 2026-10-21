package com.example.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// مكون الدرج المنسدل/المسحوب من الأسفل (Bottom Sheet Drawer)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDrawerView(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = {
            // مقبض السحب الأعلى (Drag Handle)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            content = content
        )
    }
}

// مثال على كيفية استدعاء واستخدام الدرج داخل الواجهة الرئيسية
@Composable
fun DrawerDemoScreen() {
    var isDrawerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { isDrawerOpen = true }) {
            Text("فتح الدرج (Open Drawer)")
        }

        if (isDrawerOpen) {
            CustomDrawerView(
                onDismissRequest = { isDrawerOpen = false }
            ) {
                Text(
                    text = "محتوى الدرج المخصص",
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "يمكنك وضع أي عناصر هنا مثل قائمة التطبيقات أو الإعدادات.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
