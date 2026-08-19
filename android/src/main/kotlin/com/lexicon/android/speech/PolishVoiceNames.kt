package com.lexicon.android.speech

import com.lexicon.android.cloud.CloudVoice
import com.lexicon.boundary.SpeechVoice
import com.lexicon.boundary.VoiceGender

internal val FEMALE_NAMES = listOf(
    "Agata", "Agnieszka", "Aleksandra", "Alicja", "Amelia", "Barbara",
    "Dorota", "Ewa", "Gabriela", "Hanna", "Iwona", "Joanna",
    "Julia", "Karolina", "Katarzyna", "Lena", "Magdalena", "Maja",
    "Marta", "Monika", "Natalia", "Nina", "Olga", "Paulina",
    "Renata", "Sylwia", "Urszula", "Weronika", "Wiktoria", "Zofia", "Zuzanna",
)

internal val MALE_NAMES = listOf(
    "Adam", "Alan", "Aleksander", "Antoni", "Bartosz", "Damian",
    "Filip", "Franciszek", "Grzegorz", "Henryk", "Jakub", "Jan",
    "Kacper", "Karol", "Krzysztof", "Leon", "Marcin", "Marek",
    "Michał", "Mikołaj", "Paweł", "Piotr", "Rafał", "Robert",
    "Stanisław", "Szymon", "Tomasz", "Wiktor", "Wojciech", "Zbigniew",
)

internal val NEUTRAL_NAMES = listOf(
    "Ambroży", "Bazyli", "Cibor", "Dobrogost", "Donat",
    "Jozafat", "Lubomierz", "Metody", "Miłogost",
)

private val NAMES_BY_GENDER = mapOf(
    VoiceGender.FEMALE to FEMALE_NAMES,
    VoiceGender.MALE to MALE_NAMES,
    VoiceGender.NEUTRAL to NEUTRAL_NAMES,
)

fun nameVoices(voices: List<CloudVoice>): List<SpeechVoice> =
    voices
        .groupBy { it.gender }
        .toSortedMap()
        .flatMap { (gender, ofGender) ->
            val names = NAMES_BY_GENDER[gender].orEmpty()
            ofGender.sortedBy { it.name }.mapIndexed { index, voice ->
                SpeechVoice(
                    id = voice.name,
                    displayName = names.getOrElse(index) { voice.name },
                    gender = gender,
                )
            }
        }
