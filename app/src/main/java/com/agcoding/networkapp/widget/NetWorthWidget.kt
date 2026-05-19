package com.agcoding.networkapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.agcoding.networkapp.MainActivity
import com.agcoding.networkapp.R

private val BgColor      = Color(0xFF0E1621)
private val GreenColor   = Color(0xFF76C893)
private val WhiteColor   = Color.White
private val RedColor     = Color(0xFFF09595)
private val SubtleColor  = Color(0xFF6B7280)

class NetWorthWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { SmallWidgetContent(context, data) }
    }

    @Composable
    private fun SmallWidgetContent(context: Context, data: WidgetData) {
        val changeColor = when {
            data.change == null       -> SubtleColor
            data.change >= 0          -> GreenColor
            else                      -> RedColor
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BgColor)
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // App label
                Text(
                    text  = context.getString(R.string.app_name).uppercase(),
                    style = TextStyle(
                        color      = ColorProvider(day = GreenColor, night = GreenColor),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )

                Spacer(GlanceModifier.height(4.dp))

                // Total net worth
                Text(
                    text  = data.totalFormatted,
                    style = TextStyle(
                        color      = ColorProvider(day = WhiteColor, night = WhiteColor),
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                // Change
                if (data.changeFormatted.isNotEmpty()) {
                    Spacer(GlanceModifier.height(1.dp))
                    Text(
                        text  = data.changeFormatted,
                        style = TextStyle(
                            color    = ColorProvider(day = changeColor, night = changeColor),
                            fontSize = 11.sp,
                        ),
                    )
                }

                // Account dots row (only when 2+ accounts)
                if (data.accounts.size > 1) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        data.accounts.take(4).forEachIndexed { index, account ->
                            val dotColor = try {
                                Color(android.graphics.Color.parseColor(account.colorHex))
                            } catch (e: Exception) { GreenColor }

                            if (index > 0) Spacer(GlanceModifier.width(6.dp))

                            Text(
                                text  = "●",
                                style = TextStyle(
                                    color    = ColorProvider(day = dotColor, night = dotColor),
                                    fontSize = 8.sp,
                                ),
                            )
                            Spacer(GlanceModifier.width(2.dp))
                            Text(
                                text  = account.name.take(6),
                                style = TextStyle(
                                    color    = ColorProvider(day = SubtleColor, night = SubtleColor),
                                    fontSize = 9.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
