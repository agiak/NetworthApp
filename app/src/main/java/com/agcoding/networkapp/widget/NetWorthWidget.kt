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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.agcoding.networkapp.MainActivity
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class NetWorthWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetWorthWidgetEntryPoint::class.java,
        )
        val entries = entryPoint.netWorthDao().getLatestTwoEntries()
        val currency = entryPoint.settingsRepository().getAppCurrency().first()
        provideContent { WidgetContent(context, entries, currency) }
    }

    @Composable
    private fun WidgetContent(context: Context, entries: List<NetWorthEntity>, currency: AppCurrency) {
        val latest   = entries.getOrNull(0)
        val previous = entries.getOrNull(1)
        val change   = if (latest != null && previous != null) latest.value - previous.value else null

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0E1621))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xFF76C893), night = Color(0xFF76C893)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                if (latest != null) {
                    Text(
                        text = formatCurrency(latest.value, currency.symbol),
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (change != null) {
                        val prefix = if (change >= 0) "+" else "-"
                        val changeColor = if (change >= 0) Color(0xFF5DCAA5) else Color(0xFFF09595)
                        Text(
                            text = "$prefix${formatCurrency(abs(change), currency.symbol)}",
                            style = TextStyle(
                                color = ColorProvider(day = changeColor, night = changeColor),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                } else {
                    Text(
                        text = context.getString(R.string.widget_label_no_data),
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF6B7280)),
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }

    private fun formatCurrency(value: Double, symbol: String) = "$symbol%,.0f".format(value)
}
