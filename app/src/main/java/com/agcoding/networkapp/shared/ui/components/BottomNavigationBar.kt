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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.navigation.Screen
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val navigationItems = listOf(
        NavigationItem(Screen.Home, Icons.Default.Home, stringResource(R.string.nav_home)),
        NavigationItem(Screen.Analytics, Icons.Default.DateRange, stringResource(R.string.nav_analytics)),
        NavigationItem(Screen.Settings, Icons.Default.Settings, stringResource(R.string.nav_settings))
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
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
