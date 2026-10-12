package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    data class IconItem(val key: String, val label: String, val icon: ImageVector)

    val AVAILABLE_ICONS = listOf(
        IconItem("restaurant", "Makan & Minum", Icons.Filled.Restaurant),
        IconItem("coffee", "Kopi & Kafe", Icons.Filled.Coffee),
        IconItem("shopping_cart", "Belanja", Icons.Filled.ShoppingCart),
        IconItem("directions_car", "Transportasi", Icons.Filled.DirectionsCar),
        IconItem("receipt_long", "Tagihan", Icons.Filled.ReceiptLong),
        IconItem("home", "Rumah", Icons.Filled.Home),
        IconItem("movie", "Hiburan", Icons.Filled.Movie),
        IconItem("sports_esports", "Game", Icons.Filled.SportsEsports),
        IconItem("local_hospital", "Kesehatan", Icons.Filled.LocalHospital),
        IconItem("school", "Pendidikan", Icons.Filled.School),
        IconItem("favorite", "Keluarga & Cinta", Icons.Filled.Favorite),
        IconItem("payments", "Gaji", Icons.Filled.Payments),
        IconItem("savings", "Tabungan", Icons.Filled.Savings),
        IconItem("trending_up", "Investasi", Icons.Filled.TrendingUp),
        IconItem("storefront", "Bisnis", Icons.Filled.Storefront),
        IconItem("account_balance_wallet", "Dompet", Icons.Filled.AccountBalanceWallet),
        IconItem("account_balance", "Bank", Icons.Filled.AccountBalance),
        IconItem("redeem", "Hadiah", Icons.Filled.Redeem),
        IconItem("flight", "Travel & Liburan", Icons.Filled.Flight),
        IconItem("fitness_center", "Olahraga", Icons.Filled.FitnessCenter),
        IconItem("pets", "Hewan Peliharaan", Icons.Filled.Pets),
        IconItem("work", "Pekerjaan", Icons.Filled.Work),
        IconItem("phone_android", "Pulsa & Gadget", Icons.Filled.PhoneAndroid),
        IconItem("palette", "Hobi & Seni", Icons.Filled.Palette),
        IconItem("build", "Perbaikan", Icons.Filled.Build),
        IconItem("more_horiz", "Lainnya", Icons.Filled.MoreHoriz)
    )

    val AVAILABLE_COLORS = listOf(
        "#EF4444", // Red
        "#F97316", // Orange
        "#F59E0B", // Amber
        "#10B981", // Emerald
        "#059669", // Dark Green
        "#06B6D4", // Cyan
        "#3B82F6", // Blue
        "#6366F1", // Indigo
        "#8B5CF6", // Violet
        "#EC4899", // Pink
        "#F43F5E", // Rose
        "#14B8A6", // Teal
        "#84CC16", // Lime
        "#64748B"  // Slate
    )

    fun getIcon(key: String): ImageVector {
        return AVAILABLE_ICONS.find { it.key == key }?.icon ?: Icons.Filled.Category
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
        return try {
            val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
            val colorInt = when (cleanHex.length) {
                6 -> "FF$cleanHex".toLong(16)
                8 -> cleanHex.toLong(16)
                else -> 0xFF10B981
            }
            Color(colorInt)
        } catch (e: Exception) {
            fallback
        }
    }
}
