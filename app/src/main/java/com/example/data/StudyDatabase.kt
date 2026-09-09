package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "day_records")
data class DayRecord(
    @PrimaryKey val dateKey: String,
    val bankedSeconds: Long = 0L,
    val outcome: String? = null,
    val levelAfter: Int = 0,
    val note: String = ""
)

@Dao
interface DayRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DayRecord)

    @Query("SELECT * FROM day_records WHERE dateKey = :key LIMIT 1")
    suspend fun getByDate(key: String): DayRecord?

    @Query("SELECT * FROM day_records ORDER BY dateKey DESC LIMIT :limit")
    suspend fun recent(limit: Int = 30): List<DayRecord>
}

@Database(entities = [DayRecord::class], version = 1, exportSchema = false)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dayRecordDao(): DayRecordDao

    companion object {
        @Volatile
        private var instance: StudyDatabase? = null

        fun get(context: Context): StudyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_os.db"
                ).build().also { instance = it }
            }
    }
}
