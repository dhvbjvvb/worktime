package com.worktime.checkin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInRecordDao {
    @Query("SELECT * FROM check_in_records ORDER BY dateEpochDay DESC, createdAtMillis DESC")
    fun observeAll(): Flow<List<CheckInRecordEntity>>

    @Query("SELECT * FROM check_in_records WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay DESC")
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<CheckInRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecordEntity): Long

    @Query("DELETE FROM check_in_records WHERE dateEpochDay IN (:epochDays)")
    suspend fun deleteByEpochDays(epochDays: List<Long>)
}
