package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "country_choice")
data class CountryChoiceEntity(
    @PrimaryKey val code: String,
)

@Entity(tableName = "country_progress", primaryKeys = ["code", "field"])
data class CountryProgressEntity(
    val code: String,
    val field: String,
    override val attempted: Int,
    override val correct: Int,
    override val incorrect: Int,
    override val streak: Int,
) : AttemptRecord

@Dao
interface CountryDao {
    @Query("SELECT * FROM country_choice")
    suspend fun chosen(): List<CountryChoiceEntity>

    @Query("DELETE FROM country_choice")
    suspend fun clearChosen()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChosen(rows: List<CountryChoiceEntity>)

    @Transaction
    suspend fun replaceChosen(rows: List<CountryChoiceEntity>) {
        clearChosen()
        insertChosen(rows)
    }

    @Query("SELECT * FROM country_progress")
    suspend fun progress(): List<CountryProgressEntity>

    @Query("SELECT * FROM country_progress WHERE code = :code AND field = :field LIMIT 1")
    suspend fun progressFor(
        code: String,
        field: String,
    ): CountryProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: CountryProgressEntity)
}
