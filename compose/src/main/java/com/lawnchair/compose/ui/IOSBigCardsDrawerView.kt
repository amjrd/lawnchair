package com.lawnchair.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// نموذج بيانات التطبيق
data class AppItem(
    val name: String,
    val packageName: String
)

// نموذج بيانات البطاقة الكبيرة (الفئة)
data class AppCategoryCard(
    val title: String,
    val apps: List<AppItem>
)

// مكون درج التطبيقات بتصميم بطاقات iOS الكبيرة
@Composable
fun IOSBigCardsDrawerView(
    categories: List<AppCategoryCard>,
    modifier: Modifier = Modifier,
    onAppClick: (AppItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        // شريط البحث بتصميم زجاجي آمن ومستقر
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "App Library",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // قائمة البطاقات الكبيرة الزجاجية
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { category ->
                IOSBigCardItem(category = category, onAppClick = onAppClick)
            }
        }
    }
}

// تصميم البطاقة الكبيرة الواحدة
@Composable
fun IOSBigCardItem(
    category: AppCategoryCard,
    onAppClick: (AppItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = category.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                category.apps.chunked(3).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        rowApps.forEach { app ->
                            AppTileItem(app = app, onClick = { onAppClick(app) })
                        }
                    }
                }
            }
        }
    }
}

// عنصر الأيقونة الفردية داخل البطاقة
@Composable
fun AppTileItem(
    app: AppItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}
