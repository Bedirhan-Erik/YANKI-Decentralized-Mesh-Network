package com.bedir.yanki.data.mapper

import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.local.entity.BulletinEntity
import com.bedir.yanki.Yanki.Message
import com.bedir.yanki.Yanki.EmergencySignal
import com.bedir.yanki.Yanki.BulletinPost
import com.bedir.yanki.Yanki.MeshPayload
import com.google.protobuf.ByteString
import android.util.Log

object ProtoMapper {

    fun toProto(entity: BulletinEntity): BulletinPost {
        return BulletinPost.newBuilder()
            .setPostId(entity.post_id)
            .setSenderId(entity.sender_id)
            .setSenderName(entity.sender_name)
            .setContent(entity.content)
            .setTimestamp(entity.timestamp)
            .setType(entity.type)
            .setLatitude(entity.latitude)
            .setLongitude(entity.longitude)
            .setTtl(entity.ttl)
            .build()
    }

    fun fromProto(proto: BulletinPost): BulletinEntity {
        return BulletinEntity(
            post_id = proto.postId,
            sender_id = proto.senderId,
            sender_name = proto.senderName,
            content = proto.content,
            timestamp = proto.timestamp,
            type = proto.type,
            latitude = proto.latitude,
            longitude = proto.longitude,
            ttl = proto.ttl,
            is_synced = false
        )
    }

    fun toProto(entity: MessageEntity): Message {
        val builder = Message.newBuilder()
            .setMsgId(entity.msg_id)
            .setSenderId(entity.sender_id)
            .setSenderName(entity.sender_name ?: "")
            .setReceiverId(entity.receiver_id)
            .setContentBlob(ByteString.copyFrom(entity.content_blob))
            .setTimestamp(entity.timestamp)
            .setStatus(entity.status)
            .setIsSynced(entity.is_synced)
            .setTtl(entity.ttl)
        
        entity.signature?.let {
            builder.setSignature(ByteString.copyFrom(it))
        }
        
        return builder.build()
    }

    fun fromProto(proto: Message): MessageEntity {
        return MessageEntity(
            msg_id = proto.msgId,
            sender_id = proto.senderId,
            sender_name = if (proto.senderName.isNullOrEmpty()) null else proto.senderName,
            receiver_id = proto.receiverId,
            content_blob = proto.contentBlob.toByteArray(),
            signature = if (proto.signature.isEmpty) null else proto.signature.toByteArray(),
            timestamp = proto.timestamp,
            status = proto.status,
            is_synced = true, // Sunucudan geldiğine göre senkronize
            ttl = proto.ttl
        )
    }

    fun fromProtoSOS(proto: EmergencySignal): EmergencySignalEntity {
        return EmergencySignalEntity(
            signal_id = proto.signalId,
            user_id = proto.userId,
            user_name = if (proto.userName.isNullOrEmpty()) null else proto.userName,
            latitude = proto.latitude,
            longitude = proto.longitude,
            emergency_type = proto.emergencyType,
            battery_level = proto.batteryLevel,
            hop_count = proto.hopCount,
            timestamp = proto.timestamp,
            is_synced = true,
            blood_type = if (proto.bloodType.isNullOrEmpty()) null else proto.bloodType,
            allergies = if (proto.allergies.isNullOrEmpty()) null else proto.allergies,
            medications = if (proto.medications.isNullOrEmpty()) null else proto.medications
        )
    }

    fun messageToBytes(entity: MessageEntity): ByteArray {
        return toProto(entity).toByteArray()
    }

    fun bytesToMessage(bytes: ByteArray): MessageEntity {
        val proto = Message.parseFrom(bytes)
        return MessageEntity(
            msg_id = proto.msgId,
            sender_id = proto.senderId,
            sender_name = if (proto.senderName.isNullOrEmpty()) null else proto.senderName,
            receiver_id = proto.receiverId,
            content_blob = proto.contentBlob.toByteArray(),
            signature = if (proto.signature.isEmpty) null else proto.signature.toByteArray(),
            timestamp = proto.timestamp,
            status = proto.status,
            is_synced = false,
            ttl = proto.ttl
        )
    }

    fun toProtoSOS(entity: EmergencySignalEntity): EmergencySignal {
        return EmergencySignal.newBuilder()
            .setSignalId(entity.signal_id)
            .setUserId(entity.user_id)
            .setUserName(entity.user_name ?: "")
            .setLatitude(entity.latitude)
            .setLongitude(entity.longitude)
            .setEmergencyType(entity.emergency_type)
            .setBatteryLevel(entity.battery_level)
            .setHopCount(entity.hop_count)
            .setTimestamp(entity.timestamp)
            .setBloodType(entity.blood_type ?: "")
            .setAllergies(entity.allergies ?: "")
            .setMedications(entity.medications ?: "")
            .setEmergencyContact(entity.emergency_contact ?: "")
            .build()
    }

    fun emergencyToBytes(entity: EmergencySignalEntity): ByteArray {
        return toProtoSOS(entity).toByteArray()
    }

    fun packageSOSOnly(signals: List<EmergencySignalEntity>): ByteArray {
        val payloadBuilder = MeshPayload.newBuilder()
        signals.forEach { entity ->
            payloadBuilder.addSignals(toProtoSOS(entity))
        }
        return payloadBuilder.build().toByteArray()
    }

    fun toProto(entity: UserEntity): com.bedir.yanki.Yanki.User {
        return com.bedir.yanki.Yanki.User.newBuilder()
            .setUserId(entity.user_id)
            .setUserName(entity.full_name ?: entity.username)
            .setPublicKey(ByteString.copyFrom(entity.public_key))
            .setLastSeen(entity.last_seen)
            .setIsTrusted(entity.is_trusted)
            .setBloodType(entity.blood_type ?: "")
            .setAllergies(entity.allergies ?: "")
            .setMedications(entity.medications ?: "")
            .setEmergencyContact(entity.emergency_contact ?: "")
            .build()
    }

    fun fromProto(proto: com.bedir.yanki.Yanki.User): UserEntity {
        return UserEntity(
            user_id = proto.userId,
            username = proto.userName,
            full_name = proto.userName,
            public_key = proto.publicKey.toByteArray(),
            last_seen = proto.lastSeen,
            is_trusted = proto.isTrusted,
            blood_type = if (proto.bloodType.isNullOrEmpty()) null else proto.bloodType,
            allergies = if (proto.allergies.isNullOrEmpty()) null else proto.allergies,
            medications = if (proto.medications.isNullOrEmpty()) null else proto.medications,
            emergency_contact = if (proto.emergencyContact.isNullOrEmpty()) null else proto.emergencyContact
        )
    }

    fun packageAll(
        signals: List<EmergencySignalEntity>,
        messages: List<MessageEntity>,
        users: List<UserEntity> = emptyList(),
        bulletins: List<BulletinEntity> = emptyList()
    ): ByteArray {
        val payloadBuilder = MeshPayload.newBuilder()
        signals.forEach { payloadBuilder.addSignals(toProtoSOS(it)) }
        messages.forEach { payloadBuilder.addMessages(toProto(it)) }
        users.forEach { payloadBuilder.addUsers(toProto(it)) }
        bulletins.forEach { payloadBuilder.addBulletins(toProto(it)) }
        return payloadBuilder.build().toByteArray()
    }

    fun parseIncomingPayload(data: ByteArray): ParsedPayload? {
        return try {
            val payload = MeshPayload.parseFrom(data)

            val receivedSignals = payload.signalsList.map { proto ->
                EmergencySignalEntity(
                    signal_id = proto.signalId,
                    user_id = proto.userId,
                    user_name = if (proto.userName.isNullOrEmpty()) null else proto.userName,
                    latitude = proto.latitude,
                    longitude = proto.longitude,
                    emergency_type = proto.emergencyType,
                    battery_level = proto.batteryLevel,
                    timestamp = proto.timestamp,
                    hop_count = proto.hopCount,
                    is_synced = false,
                    blood_type = if (proto.bloodType.isNullOrEmpty()) null else proto.bloodType,
                    allergies = if (proto.allergies.isNullOrEmpty()) null else proto.allergies,
                    medications = if (proto.medications.isNullOrEmpty()) null else proto.medications
                )
            }

            val receivedMessages = payload.messagesList.map { proto ->
                MessageEntity(
                    msg_id = proto.msgId,
                    sender_id = proto.senderId,
                    sender_name = if (proto.senderName.isNullOrEmpty()) null else proto.senderName,
                    receiver_id = proto.receiverId,
                    content_blob = proto.contentBlob.toByteArray(),
                    signature = if (proto.signature.isEmpty) null else proto.signature.toByteArray(),
                    timestamp = proto.timestamp,
                    status = proto.status,
                    is_synced = false,
                    ttl = proto.ttl
                )
            }

            val receivedUsers = payload.usersList.map { proto ->
                UserEntity(
                    user_id = proto.userId,
                    username = proto.userName,
                    full_name = proto.userName,
                    public_key = proto.publicKey.toByteArray(),
                    last_seen = proto.lastSeen,
                    is_trusted = proto.isTrusted,
                    blood_type = if (proto.bloodType.isNullOrEmpty()) null else proto.bloodType,
                    allergies = if (proto.allergies.isNullOrEmpty()) null else proto.allergies,
                    medications = if (proto.medications.isNullOrEmpty()) null else proto.medications,
                    emergency_contact = if (proto.emergencyContact.isNullOrEmpty()) null else proto.emergencyContact
                )
            }

            val receivedBulletins = payload.bulletinsList.map { proto ->
                BulletinEntity(
                    post_id = proto.postId,
                    sender_id = proto.senderId,
                    sender_name = proto.senderName,
                    content = proto.content,
                    timestamp = proto.timestamp,
                    type = proto.type,
                    latitude = proto.latitude,
                    longitude = proto.longitude,
                    ttl = proto.ttl,
                    is_synced = false
                )
            }

            ParsedPayload(receivedSignals, receivedMessages, receivedUsers, receivedBulletins)
        } catch (e: Exception) {
            Log.e("YANKI_MAPPER", "Gelen veri çözülemedi: ${e.message}")
            null
        }
    }
}

data class ParsedPayload(
    val signals: List<EmergencySignalEntity>,
    val messages: List<MessageEntity>,
    val users: List<UserEntity> = emptyList(),
    val bulletins: List<BulletinEntity> = emptyList()
)
