import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_signals",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class EmergencySignalEntity(
    @PrimaryKey(autoGenerate = true) val signal_id: Int = 0,
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val emergency_type: String,
    val battery_level: Int
)