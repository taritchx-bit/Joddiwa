package com.tarit.slipledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.CategoryDef
import com.tarit.slipledger.data.Wallet
import com.tarit.slipledger.data.WalletKind
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet

/** Icons a category can use; the key is what gets stored in the database. */
object CategoryIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "food" to Icons.Default.Restaurant, "coffee" to Icons.Default.Coffee, "bar" to Icons.Default.LocalBar,
        "grocery" to Icons.Default.LocalGroceryStore, "bag" to Icons.Default.ShoppingBag, "clothes" to Icons.Default.Checkroom,
        "car" to Icons.Default.DirectionsCar, "bus" to Icons.Default.DirectionsBus, "gas" to Icons.Default.LocalGasStation,
        "flight" to Icons.Default.Flight, "home" to Icons.Default.Home, "receipt" to Icons.Default.ReceiptLong,
        "wifi" to Icons.Default.Wifi, "phone" to Icons.Default.PhoneAndroid, "heart" to Icons.Default.Favorite,
        "fitness" to Icons.Default.FitnessCenter, "spa" to Icons.Default.Spa, "movie" to Icons.Default.Movie,
        "game" to Icons.Default.SportsEsports, "party" to Icons.Default.Celebration, "school" to Icons.Default.School,
        "child" to Icons.Default.ChildCare, "pet" to Icons.Default.Pets, "gift" to Icons.Default.Redeem,
        "person" to Icons.Default.Person, "store" to Icons.Default.Storefront, "work" to Icons.Default.Work,
        "payments" to Icons.Default.Payments, "savings" to Icons.Default.Savings, "invest" to Icons.Default.TrendingUp,
        "swap" to Icons.Default.SwapHoriz, "more" to Icons.Default.MoreHoriz,
    )

    fun of(key: String) = all[key] ?: Icons.Default.MoreHoriz

    val palette: List<Long> = listOf(
        0xFFFF8A5B, 0xFFF04438, 0xFFE5579B, 0xFF9E77ED, 0xFF6172F3, 0xFF3E8EF7,
        0xFF06AED4, 0xFF15B79E, 0xFF12B76A, 0xFF84CC16, 0xFFF79009, 0xFFB07A4F, 0xFF6C8EAD, 0xFF9AA19D,
    )
}

data class CategoryStyle(val icon: ImageVector, val color: Color)

/** All categories by name, provided once at the root so any composable can style a category. */
val LocalCategories = compositionLocalOf<Map<String, CategoryDef>> { emptyMap() }

/** The user's wallets, provided at the root. */
val LocalWallets = compositionLocalOf<List<Wallet>> { emptyList() }

fun walletIcon(kind: WalletKind): ImageVector = when (kind) {
    WalletKind.BANK -> Icons.Default.AccountBalance
    WalletKind.CASH -> Icons.Default.Payments
    WalletKind.EWALLET -> Icons.Default.AccountBalanceWallet
}

@Composable
fun WalletBadge(w: Wallet, size: Dp = 40.dp) = IconBadge(walletIcon(w.kind), Color(w.color), size)

private val defaultsByName = Categories.defaults.associateBy { it.name }

@Composable
fun categoryStyle(name: String): CategoryStyle {
    if (name == Categories.SELF) return CategoryStyle(CategoryIcons.of("swap"), TransferColor)
    val def = LocalCategories.current[name] ?: defaultsByName[name]
    return if (def == null) CategoryStyle(CategoryIcons.of("more"), Color(0xFF9AA19D))
    else CategoryStyle(CategoryIcons.of(def.icon), Color(def.color))
}

@Composable
fun CategoryBadge(category: String, size: Dp = 44.dp) {
    val style = categoryStyle(category)
    IconBadge(style.icon, style.color, size)
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).background(color.copy(alpha = 0.14f), RoundedCornerShape(size * 0.34f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f)) }
}
