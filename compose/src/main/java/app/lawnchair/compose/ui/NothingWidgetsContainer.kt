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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class NothingWidgetsContainerProvider : GlanceAppWidgetProvider() {
    override val glanceAppWidget: GlanceAppWidget = NothingWidgetsContainer()
}

class NothingWidgetsContainer : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(androidx.glance.unit.ColorProvider(android.graphics.Color.BLACK))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10:20",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(android.graphics.Color.WHITE),
                            fontSize = 32.sp
                        )
                    )
                    Row(
                        modifier = GlanceModifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Battery: 85%",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(android.graphics.Color.LTGRAY),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
