package com.sounak.vibebuzz.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.sounak.vibebuzz.data.entities.Song
import com.sounak.vibebuzz.other.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class MusicDatabase {

    private val firestrore = FirebaseFirestore.getInstance()
    private  val songCollection = firestrore.collection(SONG_COLLECTION)

    suspend fun getAllSongs() : List<Song> {

            return try {
                songCollection.get().await().toObjects(Song::class.java)
            } catch (e: Exception){
                emptyList()
            }
    }
}