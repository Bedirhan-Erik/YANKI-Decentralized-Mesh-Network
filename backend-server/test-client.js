const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');


// Proto dosyasını okuyoruz
const PROTO_PATH = path.join(__dirname, '../protocols/yanki.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true, longs: String, enums: String, defaults: true, oneofs: true
});
const yankiProto = grpc.loadPackageDefinition(packageDefinition).yanki;

// Sunucuya bağlanıyoruz (Senin sunucunun çalıştığı adres)
const client = new yankiProto.YankiSyncService('localhost:50051', grpc.credentials.createInsecure());

console.log("⏳ Test verileri sunucuya gönderiliyor...\n");

// --- 1. TEST: Kullanıcı Kaydı ---
client.RegisterUser({
    user_id: "test-user-999",
    user_name: "Kurtarma Ekibi Alpha",
    last_seen: Date.now(),
    is_trusted: true
}, (error, response) => {
    if (error) console.error("❌ RegisterUser Hatası:", error);
    else console.log("✅ RegisterUser Başarılı:", response.message);
});

// --- 2. TEST: SOS Acil Durum Sinyali ---
client.SendEmergencySignal({
    signal_id: 1,
    user_id: "test-user-999",
    latitude: 38.4237,  // İzmir koordinatları
    longitude: 27.1428,
    emergency_type: "Enkaz Altında - Tıbbi Yardım Gerekli",
    battery_level: 15
}, (error, response) => {
    if (error) console.error("❌ SendEmergencySignal Hatası:", error);
    else console.log("✅ SendEmergencySignal Başarılı:", response.message);
});

// --- 3. TEST: Mesaj Senkronizasyonu (Stream Testi) ---
// Stream başlattığımız için çağrıyı bir değişkene atıyoruz
const call = client.SyncMessages((error, response) => {
    if (error) console.error("❌ SyncMessages Hatası:", error);
    else console.log("✅ SyncMessages Başarılı:", response.message);
});

// Ağa arka arkaya iki mesaj yolluyoruz
call.write({
    msg_id: "msg-001",
    sender_id: "test-user-999",
    receiver_id: "BROADCAST", // Herkese açık mesaj
    timestamp: Date.now(),
    status: 1,
    is_synced: false
});

call.write({
    msg_id: "msg-002",
    sender_id: "test-user-999",
    receiver_id: "hedef-cihaz-123", // Özel mesaj
    timestamp: Date.now(),
    status: 1,
    is_synced: false
});

// Veri gönderimini bitiriyoruz
call.end();