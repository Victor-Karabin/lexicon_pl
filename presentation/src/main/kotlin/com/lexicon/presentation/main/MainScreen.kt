package com.lexicon.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.lexicon.presentation.course.PlanScreen
import com.lexicon.presentation.dashboard.DashboardScreen
import com.lexicon.presentation.presets.VocabularyScreen
import com.lexicon.presentation.settings.SettingsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    TRAININGS("Trainings", Icons.Default.School),
    VOCABULARY("Words", Icons.AutoMirrored.Filled.MenuBook),
    PLAN("Plan", Icons.AutoMirrored.Filled.EventNote),
    SETTINGS("Settings", Icons.Default.Settings),
}

private val MainTabSaver = Saver<MainTab, String>(save = { it.name }, restore = { MainTab.valueOf(it) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onTrainingSelected: (id: String) -> Unit,
    onPresetSelected: (id: String) -> Unit,
    onCourseSelected: (id: String) -> Unit,
    onProgramSelected: (id: String) -> Unit,
    onStartTraining: (training: String, wordIds: List<Long>, programId: String) -> Unit,
    onOpenCards: (programId: String) -> Unit,
    onEditWord: (id: Long) -> Unit,
    onAddWord: () -> Unit,
    onAddPreset: () -> Unit,
    onCreateProgram: () -> Unit,
    onConjugationSelected: () -> Unit,
    onTrainConjugation: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: MainTab = MainTab.DASHBOARD,
) {
    var selectedTab by rememberSaveable(stateSaver = MainTabSaver) { mutableStateOf(initialTab) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (selectedTab) {
            MainTab.TRAININGS -> TrainingsScreen(onTrainingSelected = onTrainingSelected, modifier = content)
            MainTab.DASHBOARD ->
                DashboardScreen(
                    onStartTraining = { training, words, programId ->
                        onStartTraining(training, words.map { it.value }, programId)
                    },
                    onOpenCards = onOpenCards,
                    onGoToPlan = { selectedTab = MainTab.PLAN },
                    onOpenConjugation = onTrainConjugation,
                    modifier = content,
                )
            MainTab.VOCABULARY ->
                VocabularyScreen(
                    onPresetSelected = { onPresetSelected(it.value) },
                    onEditWord = { onEditWord(it.value) },
                    onAddWord = onAddWord,
                    onAddPreset = onAddPreset,
                    modifier = content,
                )
            MainTab.PLAN ->
                PlanScreen(
                    onCourseSelected = { onCourseSelected(it.value) },
                    onProgramSelected = { onProgramSelected(it.value) },
                    onCreateProgram = onCreateProgram,
                    onConjugationSelected = onConjugationSelected,
                    modifier = content,
                )
            MainTab.SETTINGS -> SettingsScreen(modifier = content)
        }
    }
}
