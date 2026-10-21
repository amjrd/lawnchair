package com.example.launcher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ودجت الساعة والطقس بنمط Nothing OS الأنيق
@Composable
fun NothingStyleClockWidget(
    modifier: Modifier = Modifier
) {
    // استخراج الوقت والتاريخ الحالي
    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val currentDate = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())

    Box(
        modifier = modifier
            .width(300.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // القسم الأيسر: الساعة والتاريخ بنمط خطوط رقمية أو أحادية المسافة
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentTime,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    )
                    // النقطة الحمراء الأيقونية الخاصة بـ Nothing OS
                    Box(
                        modifier = Modifier
                            .padding(bottom = 14.dp, start = 4.dp)
                            .size(8.dp)
                            .background(Color(0xFFFF3333), shape = RoundedCornerShape(50%))
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = currentDate.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp
                )
            }

            // القسم الأيمن: معلومات الطقس والموقع المبسطة
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "24°",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "TATAOUINE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3333),
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
