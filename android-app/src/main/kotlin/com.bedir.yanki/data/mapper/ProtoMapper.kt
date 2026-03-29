import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import yanki.Yanki.Message // Proto dosyasından üretilen sınıflar
import yanki.Yanki.EmergencySignal

object ProtoMapper {

    // Room Entity -> Protobuf ByteArray (Göndermek için)
    fun messageToBytes(entity: MessageEntity): ByteArray {
        return Message.newBuilder()
            .setMsgId(entity.msg_id)
            .setSenderId(entity.sender_id)
            .setReceiverId(entity.receiver_id)
            .setContentBlob(com.google.protobuf.ByteString.copyFrom(entity.content_blob))
            .setTimestamp(entity.timestamp)
            .setStatus(entity.status)
            .build()
            .toByteArray()
    }

    // Protobuf ByteArray -> Room Entity (Almak için)
    fun bytesToMessage(bytes: ByteArray): MessageEntity {
        val proto = Message.parseFrom(bytes)
        return MessageEntity(
            msg_id = proto.msgId,
            sender_id = proto.senderId,
            receiver_id = proto.receiverId,
            content_blob = proto.contentBlob.toByteArray(),
            timestamp = proto.timestamp,
            status = proto.status,
            is_synced = false
        )
    }

    // SOS Sinyalini Paketle
    fun emergencyToBytes(entity: EmergencySignalEntity): ByteArray {
        return EmergencySignal.newBuilder()
            .setUserId(entity.user_id)
            .setLatitude(entity.latitude)
            .setLongitude(entity.longitude)
            .setEmergencyType(entity.emergency_type)
            .setBatteryLevel(entity.battery_level)
            .build()
            .toByteArray()
    }
}