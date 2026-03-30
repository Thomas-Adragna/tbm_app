package com.example.tbmapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val DETAIL = "detail/{stationId}"

    fun detail(stationId: Int) = "detail/$stationId"
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Liste : BottomNavItem(Routes.HOME, "Liste", Icons.Default.List)
    object Carte : BottomNavItem(Routes.MAP, "Carte", Icons.Default.Map)
}