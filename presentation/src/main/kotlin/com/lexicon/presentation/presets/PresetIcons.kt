package com.lexicon.presentation.presets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

private val PRESET_ICONS: Map<String, ImageVector> = mapOf(
    "account_balance" to Icons.Default.AccountBalance,
    "auto_stories" to Icons.Default.Bookmarks,
    "badge" to Icons.Default.BusinessCenter,
    "bug_report" to Icons.Default.BugReport,
    "business_center" to Icons.Default.BusinessCenter,
    "calendar_month" to Icons.Default.Schedule,
    "category" to Icons.Default.Category,
    "chat" to Icons.Default.Chat,
    "checkroom" to Icons.Default.Checkroom,
    "cleaning_services" to Icons.Default.CleaningServices,
    "code" to Icons.Default.Code,
    "computer" to Icons.Default.Computer,
    "directions_bus" to Icons.Default.DirectionsBus,
    "edit" to Icons.Default.Edit,
    "emergency" to Icons.Default.LocalHospital,
    "explore" to Icons.Default.Explore,
    "face" to Icons.Default.Face,
    "family_restroom" to Icons.Default.FamilyRestroom,
    "favorite" to Icons.Default.Favorite,
    "favorite_border" to Icons.Default.FavoriteBorder,
    "fitness_center" to Icons.Default.FitnessCenter,
    "flight" to Icons.Default.Flight,
    "flutter_dash" to Icons.Default.Pets,
    "forest" to Icons.Default.Forest,
    "gavel" to Icons.Default.Gavel,
    "groups" to Icons.Default.Groups,
    "home" to Icons.Default.Home,
    "hotel" to Icons.Default.Hotel,
    "how_to_vote" to Icons.Default.Gavel,
    "interests" to Icons.Default.Interests,
    "language" to Icons.Default.Language,
    "local_cafe" to Icons.Default.LocalCafe,
    "local_florist" to Icons.Default.LocalFlorist,
    "local_hospital" to Icons.Default.LocalHospital,
    "local_pharmacy" to Icons.Default.LocalPharmacy,
    "local_police" to Icons.Default.LocalPolice,
    "location_city" to Icons.Default.LocationCity,
    "medication" to Icons.Default.Medication,
    "menu_book" to Icons.Default.Bookmarks,
    "mood" to Icons.Default.Mood,
    "movie" to Icons.Default.Movie,
    "music_note" to Icons.Default.MusicNote,
    "nature" to Icons.Default.Forest,
    "palette" to Icons.Default.Palette,
    "park" to Icons.Default.Park,
    "person_add" to Icons.Default.PersonAdd,
    "pets" to Icons.Default.Pets,
    "pin" to Icons.Default.Pin,
    "psychology" to Icons.Default.Psychology,
    "public" to Icons.Default.Public,
    "restaurant" to Icons.Default.Restaurant,
    "sailing" to Icons.Default.Sailing,
    "savings" to Icons.Default.Savings,
    "schedule" to Icons.Default.Schedule,
    "school" to Icons.Default.School,
    "set_meal" to Icons.Default.SetMeal,
    "shopping_cart" to Icons.Default.ShoppingCart,
    "smartphone" to Icons.Default.Smartphone,
    "soup_kitchen" to Icons.Default.Restaurant,
    "sports_esports" to Icons.Default.SportsEsports,
    "sports_soccer" to Icons.Default.SportsSoccer,
    "storefront" to Icons.Default.Storefront,
    "terrain" to Icons.Default.Terrain,
    "tour" to Icons.Default.Tour,
    "trending_up" to Icons.Default.TrendingUp,
    "waving_hand" to Icons.Default.WavingHand,
    "wb_sunny" to Icons.Default.WbSunny,
    "yard" to Icons.Default.Yard,
)

fun presetIconFor(name: String?): ImageVector = PRESET_ICONS[name] ?: Icons.Default.Category
