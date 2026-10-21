package app.lawnchair.compose.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetProvider
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class NothingAlarmWidgetProvider : GlanceAppWidgetProvider() {
    override val glanceAppWidget: GlanceAppWidget = NothingAlarmWidget()
}

class NothingAlarmWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(androidx.glance.unit.ColorProvider(android.graphics.Color.BLACK))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Alarm: 07:00",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(android.graphics.Color.WHITE),
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    }
}
