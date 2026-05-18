package com.agcoding.networkapp.shared.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.navigation.AnalyticsRoute
import com.agcoding.networkapp.shared.navigation.HomeRoute
import com.agcoding.networkapp.shared.navigation.SettingsRoute
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    val navigationItems = listOf(
        NavigationItem(HomeRoute, Icons.Default.Home, stringResource(R.string.nav_home)),
        NavigationItem(AnalyticsRoute, Icons.Default.DateRange, stringResource(R.string.nav_analytics)),
        NavigationItem(SettingsRoute, Icons.Default.Settings, stringResource(R.string.nav_settings)),
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        navigationItems.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo<HomeRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarPreview() {
    NetWorthTheme {
        BottomNavigationBar(navController = rememberNavController())
    }
}
