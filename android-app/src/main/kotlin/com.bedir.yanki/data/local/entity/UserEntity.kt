import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val user_id: String,
    val user_name: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val public_key: ByteArray?,
    val last_seen: Long,
    val is_trusted: Boolean = false
)