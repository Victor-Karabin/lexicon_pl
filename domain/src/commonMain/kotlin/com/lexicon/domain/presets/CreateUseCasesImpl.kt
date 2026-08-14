package com.lexicon.domain.presets

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.Translator
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.polishTranscription
import com.lexicon.interactors.presets.CreatePresetUseCase
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetDraftException
import com.lexicon.interactors.presets.PresetDraftProblem
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.UpdateWordUseCase
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.interactors.presets.WordDraftException
import com.lexicon.interactors.presets.WordDraftProblem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** How many pictures the learner is offered at a time. */
private const val IMAGE_CANDIDATES = 3

class CreateWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val presetRepository: VocabularyPresetRepository,
    private val imageProvider: ImageProvider,
) : CreateWordUseCase {
    override suspend fun invoke(
        text: String,
        translation: String,
        imageUrl: String?,
        presetIds: List<PresetId>,
    ): Result<PresetWord> {
        val polish = text.trim()
        val english = translation.trim()

        if (polish.isEmpty()) return Result.failure(WordDraftException(WordDraftProblem.MISSING_TEXT))
        if (english.isEmpty()) return Result.failure(WordDraftException(WordDraftProblem.MISSING_TRANSLATION))
        if (vocabularyRepository.findWordByText(polish) != null) {
            return Result.failure(WordDraftException(WordDraftProblem.ALREADY_EXISTS))
        }

        val word = vocabularyRepository.createWord(
            text = polish,
            translation = english,
            // Derived rather than asked for, so a hand-added word reads the same way
            // in the list and the trainings as every shipped one.
            transcription = polishTranscription(polish),
        )

        for (presetId in presetIds.distinct()) {
            presetRepository.setWordInPreset(presetId = presetId.value, wordId = word.id, isMember = true)
        }

        // Keyed on the English translation because that is what the picture
        // trainings search by; pinning anywhere else would not reach them.
        if (!imageUrl.isNullOrBlank()) imageProvider.pinImage(query = english, imageUrl = imageUrl)

        return Result.success(word.toPresetWord())
    }
}

class UpdateWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val presetRepository: VocabularyPresetRepository,
    private val imageProvider: ImageProvider,
) : UpdateWordUseCase {
    override suspend fun invoke(
        id: VocabularyId,
        text: String,
        translation: String,
        imageUrl: String?,
        presetIds: List<PresetId>,
    ): Result<PresetWord> {
        val polish = text.trim()
        val english = translation.trim()

        if (polish.isEmpty()) return Result.failure(WordDraftException(WordDraftProblem.MISSING_TEXT))
        if (english.isEmpty()) return Result.failure(WordDraftException(WordDraftProblem.MISSING_TRANSLATION))
        // A word is allowed to keep its own spelling; it is only a clash if some
        // other word already has it.
        val clash = vocabularyRepository.findWordByText(polish)
        if (clash != null && clash.id != id.value) {
            return Result.failure(WordDraftException(WordDraftProblem.ALREADY_EXISTS))
        }

        val word = vocabularyRepository.updateWord(
            id = id.value,
            text = polish,
            translation = english,
            transcription = polishTranscription(polish),
        )

        // Only the difference is written: setWordInPreset records an override per
        // call, and rewriting all seventy-odd would bury the handful that changed.
        val wanted = presetIds.mapTo(mutableSetOf()) { it.value }
        val current = presetRepository.getPresetIdsForWord(id.value).toSet()
        for (presetId in wanted - current) {
            presetRepository.setWordInPreset(presetId = presetId, wordId = id.value, isMember = true)
        }
        for (presetId in current - wanted) {
            presetRepository.setWordInPreset(presetId = presetId, wordId = id.value, isMember = false)
        }

        if (!imageUrl.isNullOrBlank()) imageProvider.pinImage(query = english, imageUrl = imageUrl)

        return Result.success(word.toPresetWord())
    }
}

class GetWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : GetWordUseCase {
    override suspend fun invoke(id: VocabularyId): PresetWord? = vocabularyRepository.getWord(id.value)?.toPresetWord()
}

class CreatePresetUseCaseImpl(
    private val presetRepository: VocabularyPresetRepository,
) : CreatePresetUseCase {
    override suspend fun invoke(
        title: String,
        description: String,
        icon: String?,
        color: String?,
        wordIds: List<VocabularyId>,
    ): Result<VocabularyPreset> {
        val name = title.trim()
        if (name.isEmpty()) return Result.failure(PresetDraftException(PresetDraftProblem.MISSING_TITLE))

        // Stored under the default language rather than the phone's: the learner
        // typed one name, and claiming it is the Polish one when the interface is in
        // English would put the wrong string in front of them on a language change.
        val preset = presetRepository.createPreset(
            title = mapOf(LocalizedText.DEFAULT_LANGUAGE to name),
            description = description.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { mapOf(LocalizedText.DEFAULT_LANGUAGE to it) }
                .orEmpty(),
            icon = icon,
            color = color,
            wordIds = wordIds.map { it.value },
        )

        val category = presetRepository
            .getCategories()
            .firstOrNull { it.id == preset.categoryId }
            ?.toCategory()
            ?: error("preset ${preset.id} was filed under an unknown category ${preset.categoryId}")

        return Result.success(preset.toPreset(category))
    }
}

class TranslateWordUseCaseImpl(
    private val translator: Translator,
) : TranslateWordUseCase {
    override suspend fun invoke(
        text: String,
        toPolish: Boolean,
    ): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val direction = if (toPolish) TranslationDirection.EN_TO_PL else TranslationDirection.PL_TO_EN
        return translator.translate(trimmed, direction)?.trim()?.takeIf { it.isNotEmpty() }
    }
}

class SearchImageCandidatesUseCaseImpl(
    private val imageProvider: ImageProvider,
) : SearchImageCandidatesUseCase {
    override suspend fun invoke(
        query: String,
        skip: Int,
    ): ImmutableList<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return persistentListOf()
        return imageProvider.searchImages(trimmed, count = IMAGE_CANDIDATES, skip = skip).toImmutableList()
    }
}
