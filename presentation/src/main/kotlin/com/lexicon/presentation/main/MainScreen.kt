package com.lexicon.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.lexicon.presentation.settings.SettingsScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    TRAININGS("Trainings", Icons.Default.School),
    VOCABULARY("Vocabulary", Icons.AutoMirrored.Filled.MenuBook),
    STATISTICS("Statistics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onTrainingSelected: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(MainTab.TRAININGS) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (selectedTab) {
            MainTab.TRAININGS -> TrainingsScreen(onTrainingSelected = onTrainingSelected, modifier = content)
            MainTab.DASHBOARD -> ComingSoonScreen(MainTab.DASHBOARD.label, modifier = content)
            MainTab.VOCABULARY -> ComingSoonScreen(MainTab.VOCABULARY.label, modifier = content)
            MainTab.STATISTICS -> ComingSoonScreen(MainTab.STATISTICS.label, modifier = content)
            MainTab.SETTINGS -> SettingsScreen(modifier = content)
        }
    }
}
