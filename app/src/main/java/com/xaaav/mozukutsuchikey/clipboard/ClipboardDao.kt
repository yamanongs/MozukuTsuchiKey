package com.xaaav.mozukutsuchikey.clipboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, timestamp DESC")
    fun observeAll(): Flow<List<ClipboardEntity>>

    @Insert
    suspend fun insert(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_items WHERE timestamp < :expiryTime AND pinned = 0")
    suspend fun deleteExpired(expiryTime: Long)

    @Query("UPDATE clipboard_items SET pinned = :pinned WHERE id = :id")
    suspend fun updatePin(id: Long, pinned: Boolean)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clipboard_items WHERE text = :text AND pinned = 0")
    suspend fun deleteByText(text: String)
}
