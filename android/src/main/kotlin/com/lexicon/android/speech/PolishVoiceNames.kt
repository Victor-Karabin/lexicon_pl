package com.lexicon.android.speech

import com.lexicon.android.cloud.CloudVoice
import com.lexicon.boundary.SpeechVoice
import com.lexicon.boundary.VoiceGender

/**
 * Polish given names for Cloud voices, carried over from the earlier Lexicon app.
 *
 * Cloud voices declare their gender, so unlike the device's own voices these names can be
 * right rather than merely stable: the voices of one gender are matched against the names
 * of that gender. A woman's voice cannot come out called Piotr.
 *
 * Kept alphabetical, and longer than the voice list Google currently offers for Polish
 * (16 women, 18 men), so every voice gets a name rather than falling back to its id.
 */
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

/**
 * Names a set of voices, gender by gender.
 *
 * Both sides are sorted before being paired — the voices by the engine's own name, the
 * names alphabetically — so the same device always produces the same pairing and a voice
 * keeps its name between launches. Voices beyond the supply of names fall back to the
 * engine's name rather than borrowing one already in use.
 */
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
