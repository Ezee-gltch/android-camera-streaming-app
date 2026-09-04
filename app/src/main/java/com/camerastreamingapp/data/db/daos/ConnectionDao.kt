package com.camerastreamingapp.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camerastreamingapp.data.db.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity): Long

    @Update
    suspend fun update(connection: ConnectionEntity)

    @Query("SELECT * FROM connections WHERE connectionId = :connectionId")
    suspend fun getById(connectionId: Long): ConnectionEntity?

    @Query("SELECT * FROM connections WHERE cameraId = :cameraId")
    fun getByCameraId(cameraId: Long): Flow<ConnectionEntity?>

    @Query("SELECT * FROM connections")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE status IN ('DISCONNECTED', 'FAILED')")
    suspend fun findDisconnectedConnections(): List<ConnectionEntity>

    @Query(
        "UPDATE connections SET status = :status, lastStatusChange = :timestamp, failureCount = :failureCount, nextRetryTime = :nextRetryTime WHERE cameraId = :cameraId"
    )
    suspend fun updateStatusByCameraId(
        cameraId: Long,
        status: String,
        timestamp: Long,
        failureCount: Int,
        nextRetryTime: Long?
    )

    @Query("DELETE FROM connections WHERE connectionId = :connectionId")
    suspend fun deleteById(connectionId: Long)
}
