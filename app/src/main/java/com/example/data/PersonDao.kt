package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM discovered_people ORDER BY lastSeenTimestamp DESC")
    fun getAllPeople(): Flow<List<DiscoveredPerson>>

    @Query("SELECT * FROM discovered_people WHERE isSaved = 1 ORDER BY lastSeenTimestamp DESC")
    fun getSavedPeople(): Flow<List<DiscoveredPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: DiscoveredPerson): Long

    @Query("UPDATE discovered_people SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: Int, isSaved: Boolean)

    @Query("UPDATE discovered_people SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Int, notes: String)

    @Query("DELETE FROM discovered_people WHERE id = :id")
    suspend fun deletePerson(id: Int)

    @Query("SELECT * FROM discovered_people WHERE instagramId = :instagramId LIMIT 1")
    suspend fun getPersonByInstagramId(instagramId: String): DiscoveredPerson?
}
