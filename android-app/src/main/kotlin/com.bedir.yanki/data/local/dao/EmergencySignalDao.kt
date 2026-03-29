import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmergencySignalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: EmergencySignalEntity)

    @Query("SELECT * FROM emergency_signals ORDER BY signal_id DESC")
    suspend fun getAllSignals(): List<EmergencySignalEntity>
}