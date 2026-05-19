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
import androidx.glance.layout.fillMaxHeight
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

private val BgColor     = Color(0xFF0E1621)
private val CardBg      = Color(0xFF162032)
private val GreenColor  = Color(0xFF76C893)
private val WhiteColor  = Color.White
private val RedColor    = Color(0xFFF09595)
private val SubtleColor = Color(0xFF6B7280)
private val DivColor    = Color(0xFF1E2D42)

class NetWorthDetailWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { DetailWidgetContent(context, data) }
    }

    @Composable
    private fun DetailWidgetContent(context: Context, data: WidgetData) {
        val changeColor = when {
            data.change == null -> SubtleColor
            data.change >= 0    -> GreenColor
            else                -> RedColor
        }
        val hasMultipleAccounts = data.accounts.size > 1

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BgColor)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // ── Header ─────────────────────────────────────────────
                Text(
                    text  = context.getString(R.string.label_net_worth_upper),
                    style = TextStyle(
                        color      = ColorProvider(day = GreenColor, night = GreenColor),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )

                Spacer(GlanceModifier.height(4.dp))

                Text(
                    text  = data.totalFormatted,
                    style = TextStyle(
                        color      = ColorProvider(day = WhiteColor, night = WhiteColor),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )

                if (data.changeFormatted.isNotEmpty()) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text  = data.changeFormatted,
                        style = TextStyle(
                            color    = ColorProvider(day = changeColor, night = changeColor),
                            fontSize = 12.sp,
                        ),
                    )
                }

                // ── Divider ────────────────────────────────────────────
                if (hasMultipleAccounts) {
                    Spacer(GlanceModifier.height(12.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DivColor),
                    ) {}
                    Spacer(GlanceModifier.height(10.dp))

                    // ── Account rows ───────────────────────────────────
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                    ) {
                        data.accounts.forEach { account ->
                            val dotColor = try {
                                Color(android.graphics.Color.parseColor(account.colorHex))
                            } catch (e: Exception) { GreenColor }

                            Row(
                                modifier          = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text  = "●",
                                    style = TextStyle(
                                        color    = ColorProvider(day = dotColor, night = dotColor),
                                        fontSize = 9.sp,
                                    ),
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                Text(
                                    text     = account.name,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style    = TextStyle(
                                        color    = ColorProvider(day = SubtleColor, night = SubtleColor),
                                        fontSize = 11.sp,
                                    ),
                                )
                                Text(
                                    text  = account.formattedBalance,
                                    style = TextStyle(
                                        color      = ColorProvider(day = WhiteColor, night = WhiteColor),
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }

                    // ── Allocation bar ─────────────────────────────────
                    Spacer(GlanceModifier.height(10.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(5.dp),
                    ) {
                        data.accounts.forEach { account ->
                            val barColor = try {
                                Color(android.graphics.Color.parseColor(account.colorHex))
                            } catch (e: Exception) { GreenColor }
                            val weight = account.percentage.coerceAtLeast(0.01f)
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .background(barColor),
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
