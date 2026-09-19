package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.lexicon.boundary.VocabularyCourseBoundary
import kotlinx.coroutines.flow.Flow

private const val QUEUE_SEPARATOR = ","

const val VOCABULARY_COURSE_ROW = 0

@Entity(tableName = "vocabulary_course")
data class VocabularyCourseEntity(
    @PrimaryKey val id: Int = VOCABULARY_COURSE_ROW,
    val newWordsADay: Int,
    val reviewsADay: Int,
    val queue: String,
    val position: Int,
    val round: Int,
    val cardsSeenRound: Int,
)

@Dao
interface VocabularyCourseDao {
    @Query("SELECT * FROM vocabulary_course WHERE id = $VOCABULARY_COURSE_ROW")
    fun observe(): Flow<VocabularyCourseEntity?>

    @Query("SELECT * FROM vocabulary_course WHERE id = $VOCABULARY_COURSE_ROW")
    suspend fun get(): VocabularyCourseEntity?

    @Upsert
    suspend fun save(course: VocabularyCourseEntity)
}

fun VocabularyCourseEntity.toBoundary(): VocabularyCourseBoundary =
    VocabularyCourseBoundary(
        newWordsADay = newWordsADay,
        reviewsADay = reviewsADay,
        queue = queue.split(QUEUE_SEPARATOR).filter { it.isNotBlank() },
        position = position,
        round = round,
        cardsSeenRound = cardsSeenRound,
    )

fun VocabularyCourseBoundary.toEntity(): VocabularyCourseEntity =
    VocabularyCourseEntity(
        newWordsADay = newWordsADay,
        reviewsADay = reviewsADay,
        queue = queue.joinToString(QUEUE_SEPARATOR),
        position = position,
        round = round,
        cardsSeenRound = cardsSeenRound,
    )
