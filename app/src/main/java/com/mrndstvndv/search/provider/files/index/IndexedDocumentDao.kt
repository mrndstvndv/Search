package com.mrndstvndv.search.provider.files.index

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface IndexedDocumentDao {

    @Query("DELETE FROM indexed_documents WHERE rootId = :rootId")
    suspend fun deleteForRoot(rootId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<IndexedDocumentEntity>)

    @Upsert
    suspend fun upsertAll(entities: List<IndexedDocumentEntity>)

    @Query("SELECT * FROM indexed_documents WHERE rootId = :rootId")
    suspend fun getAllForRoot(rootId: String): List<IndexedDocumentEntity>

    @Query("DELETE FROM indexed_documents WHERE documentUri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query(
        """
        SELECT * FROM indexed_documents
        WHERE rootId IN (:rootIds)
        AND (displayName LIKE :query ESCAPE '\' OR relativePath LIKE :query ESCAPE '\')
        ORDER BY isDirectory DESC, lastModified DESC
        LIMIT :limit
        """
    )
    suspend fun search(rootIds: List<String>, query: String, limit: Int): List<IndexedDocumentEntity>
}
