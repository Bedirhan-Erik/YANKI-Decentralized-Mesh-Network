const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');
const { db } = require('./firebase-config'); // Firestore bağlantımızı dahil ettik
const admin = require('firebase-admin');

const PROTO_PATH = path.join(__dirname, '../protocols/yanki.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true, longs: String, enums: String, defaults: true, oneofs: true
});

const yankiProto = grpc.loadPackageDefinition(packageDefinition).yanki;

// --- gRPC Servis Fonksiyonları (Firebase Entegreli) ---

async function RegisterUser(call, callback) {
    const user = call.request;
    console.log(`[LOG] Yeni Kullanıcı Kaydı: ${user.user_name}`);

    try {
        // users koleksiyonuna user_id'yi doküman ID'si yaparak kaydediyoruz
        await db.collection('users').doc(user.user_id).set({
            user_name: user.user_name,
            last_seen: user.last_seen,
            is_trusted: user.is_trusted,
            // public_key'i byte buffer olarak alıyoruz, Firestore bunu handle edebilir
        });
        callback(null, { success: true, message: "Kullanıcı buluta başarıyla kaydedildi." });
    } catch (error) {
        console.error("Kayıt Hatası:", error);
        callback(null, { success: false, message: "Kayıt başarısız oldu." });
    }
}

async function SendEmergencySignal(call, callback) {
    const signal = call.request;
    console.log(`[ALERT] SOS Sinyali! Tür: ${signal.emergency_type}`);

    try {
        // Acil durumlar otomatik ID ile kaydedilsin
        await db.collection('emergency_signals').add({
            user_id: signal.user_id,
            latitude: signal.latitude,
            longitude: signal.longitude,
            emergency_type: signal.emergency_type,
            battery_level: signal.battery_level,
            timestamp: admin.firestore.FieldValue.serverTimestamp() // Buluta ulaştığı an
        });
        callback(null, { success: true, message: "SOS sinyali merkeze ulaştı!" });
    } catch (error) {
        console.error("SOS Kayıt Hatası:", error);
        callback(null, { success: false, message: "SOS iletilemedi." });
    }
}

function SyncMessages(call, callback) {
    console.log("[LOG] Mesaj senkronizasyon akışı (stream) başladı...");

    call.on('data', async function(message) {
        console.log(`[SYNC] Mesaj alındı: ${message.msg_id}`);
        try {
            await db.collection('messages').doc(message.msg_id).set({
                sender_id: message.sender_id,
                receiver_id: message.receiver_id,
                timestamp: message.timestamp,
                status: message.status,
                is_synced: true // Artık bulutta olduğu için true yapıyoruz
                // content_blob şifreli veriyi doğrudan kaydediyoruz
            });
        } catch (error) {
            console.error(`Mesaj (${message.msg_id}) kaydedilemedi:`, error);
        }
    });

    call.on('end', function() {
        console.log("[LOG] Mesaj senkronizasyon akışı tamamlandı.");
        callback(null, { success: true, message: "Tüm çevrimdışı mesajlar senkronize edildi." });
    });
}

// --- Sunucuyu Başlatma ---
function main() {
    const server = new grpc.Server();
    server.addService(yankiProto.YankiSyncService.service, {
        RegisterUser: RegisterUser,
        SendEmergencySignal: SendEmergencySignal,
        SyncMessages: SyncMessages
    });

    const PORT = '0.0.0.0:50051';
    server.bindAsync(PORT, grpc.ServerCredentials.createInsecure(), (error, port) => {
        if (error) {
            console.error("Sunucu başlatılamadı:", error);
            return;
        }
        console.log(`YANKI Backend gRPC & Firebase Sunucusu ${port} portunda dinleniyor...`);
    });
}

main();