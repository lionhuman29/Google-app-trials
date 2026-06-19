package com.example.data

import kotlinx.coroutines.flow.Flow

class PersonRepository(private val personDao: PersonDao) {
    val allPeople: Flow<List<DiscoveredPerson>> = personDao.getAllPeople()
    val savedPeople: Flow<List<DiscoveredPerson>> = personDao.getSavedPeople()

    suspend fun insert(person: DiscoveredPerson) {
        val existing = personDao.getPersonByInstagramId(person.instagramId)
        if (existing != null) {
            val updated = person.copy(
                id = existing.id,
                isSaved = existing.isSaved,
                notes = existing.notes.ifBlank { person.notes }
            )
            personDao.insertPerson(updated)
        } else {
            personDao.insertPerson(person)
        }
    }

    suspend fun updateSavedStatus(id: Int, isSaved: Boolean) {
        personDao.updateSavedStatus(id, isSaved)
    }

    suspend fun updateNotes(id: Int, notes: String) {
        personDao.updateNotes(id, notes)
    }

    suspend fun delete(id: Int) {
        personDao.deletePerson(id)
    }
}
