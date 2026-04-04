package com.bedir.yanki.data.mapper

import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.Yanki.Message
import com.bedir.yanki.Yanki.EmergencySignal
import com.bedir.yanki.Yanki.MeshPayload
import com.google.protobuf.ByteString
import android.util.Log

object ProtoMapper {

    fun toProto(entity: MessageEntity): Message {
        return Message.newBuilder()
            .setMsgId(entity.msg_id)
            .setSenderId(entity.sender_id)
            .setReceiverId(entity.receiver_id)
            .setContentBlob(ByteString.copyFrom(entity.content_blob))
            .setTimestamp(entity.timestamp)
            .setStatus(entity.status)
            .setIsSynced(entity.is_synced)
            .setTtl(entity.ttl)
            .build()
    }

    fun messageToBytes(entity: MessageEntity): ByteArray {
        return toProto(entity).toByteArray()
    }

    fun bytesToMessage(bytes: ByteArray): MessageEntity {
        val proto = Message.parseFrom(bytes)
        return MessageEntity(
            msg_id = proto.msgId,
            sender_id = proto.senderId,
            receiver_id = proto.receiverId,
            content_blob = proto.contentBlob.toByteArray(),
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
            .setLatitude(entity.latitude)
            .setLongitude(entity.longitude)
            .setEmergencyType(entity.emergency_type)
            .setBatteryLevel(entity.battery_level)
            .setHopCount(entity.hop_count)
            .setTimestamp(entity.timestamp)
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

    fun packageAll(signals: List<EmergencySignalEntity>, messages: List<MessageEntity>): ByteArray {
        val payloadBuilder = MeshPayload.newBuilder()
        signals.forEach { payloadBuilder.addSignals(toProtoSOS(it)) }
        messages.forEach { payloadBuilder.addMessages(toProto(it)) }
        return payloadBuilder.build().toByteArray()
    }

    fun parseIncomingPayload(data: ByteArray): ParsedPayload? {
        return try {
            val payload = MeshPayload.parseFrom(data)

            val receivedSignals = payload.signalsList.map { proto ->
                EmergencySignalEntity(
                    signal_id = proto.signalId,
                    user_id = proto.userId,
                    latitude = proto.latitude,
                    longitude = proto.longitude,
                    emergency_type = proto.emergencyType,
                    battery_level = proto.batteryLevel,
                    timestamp = proto.timestamp,
                    hop_count = proto.hopCount,
                    is_synced = false
                )
            }

            val receivedMessages = payload.messagesList.map { proto ->
                MessageEntity(
                    msg_id = proto.msgId,
                    sender_id = proto.senderId,
                    receiver_id = proto.receiverId,
                    content_blob = proto.contentBlob.toByteArray(),
                    timestamp = proto.timestamp,
                    status = proto.status,
                    is_synced = false,
                    ttl = proto.ttl
                )
            }

            ParsedPayload(receivedSignals, receivedMessages)
        } catch (e: Exception) {
            Log.e("YANKI_MAPPER", "Gelen veri çözülemedi: ${e.message}")
            null
        }
    }
}

data class ParsedPayload(
    val signals: List<EmergencySignalEntity>,
    val messages: List<MessageEntity>
)
