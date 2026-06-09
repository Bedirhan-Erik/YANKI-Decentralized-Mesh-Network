const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');
const http = require('http');
const fs   = require('fs');
const { db } = require('./firebase-config');
const admin = require('firebase-admin');

const PROTO_PATH = path.join(__dirname, 'protos/yanki.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true, longs: String, enums: String, defaults: true, oneofs: true
});

const yankiProto = grpc.loadPackageDefinition(packageDefinition).yanki;

// --- gRPC Servis Fonksiyonları ---

async function RegisterUser(call, callback) {
    const user = call.request;
    console.log(`[USER] Kayıt Talebi: ${user.user_name} (${user.user_id})`);

    try {
        await db.collection('users').doc(user.user_id).set({
            user_name: user.user_name,
            last_seen: admin.firestore.Timestamp.fromMillis(parseInt(user.last_seen)),
            is_trusted: user.is_trusted || false,
            public_key: user.public_key, // Byte dizisi olarak saklanır
            updated_at: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true }); // Kullanıcı varsa bilgilerini güncelle (last_seen vb.)

        callback(null, { success: true, message: "Kullanıcı başarıyla kaydedildi/güncellendi." });
    } catch (error) {
        console.error("Kayıt Hatası:", error);
        callback(null, { success: false, message: "Sunucu hatası: Kayıt yapılamadı." });
    }
}

const SOS_RATE_LIMIT_MS = 10 * 60 * 1000; // 10 dakika

async function SendEmergencySignal(call, callback) {
    const signal = call.request;
    console.log(`[ALERT] SOS! Tür: ${signal.emergency_type} - Pil: %${signal.battery_level} - ID: ${signal.signal_id}`);

    try {
        if (!signal.signal_id || !signal.user_id) {
            throw new Error("Eksik veri: signal_id veya user_id bulunamadı.");
        }

        const sosData = {
            user_id: signal.user_id,
            user_name: signal.user_name || "Bilinmiyor",
            location: new admin.firestore.GeoPoint(Number(signal.latitude) || 0, Number(signal.longitude) || 0),
            emergency_type: signal.emergency_type || "Bilinmiyor",
            battery_level: Number(signal.battery_level) || 0,
            mesh_hops: Number(signal.hop_count) || 0,
            created_at: admin.firestore.Timestamp.fromMillis(Number(signal.timestamp) || Date.now()),
            synced_at: admin.firestore.FieldValue.serverTimestamp(),
            blood_type: signal.blood_type || "",
            allergies: signal.allergies || "",
            medications: signal.medications || "",
            emergency_contact: signal.emergency_contact || ""
        };

        // Rate-limit: aynı kullanıcının son 10 dakika içinde aktif sinyali varsa onu güncelle
        const rateLimitThreshold = admin.firestore.Timestamp.fromMillis(Date.now() - SOS_RATE_LIMIT_MS);
        const recentSnapshot = await db.collection('emergency_signals')
            .where('user_id', '==', signal.user_id)
            .where('created_at', '>', rateLimitThreshold)
            .limit(1)
            .get();

        let targetDocId = signal.signal_id;
        if (!recentSnapshot.empty) {
            targetDocId = recentSnapshot.docs[0].id;
            console.log(`⚡ Rate-limit: ${signal.user_id} son 10dk içinde sinyal gönderdi, güncelleniyor: ${targetDocId}`);
        }

        await db.collection('emergency_signals').doc(targetDocId).set(sosData);

        console.log(`🚨 SOS Firestore'a işlendi: ${targetDocId} (${signal.emergency_type})`);
        callback(null, { success: true, message: "Acil durum sinyali Firebase'e işlendi." });
    } catch (error) {
        console.error("❌ SOS Kayıt Hatası:", error);
        callback(null, { success: false, message: `SOS Yazma Hatası: ${error.message}` });
    }
}

async function SyncBulletin(call, callback) {
    const post = call.request;
    console.log(`[BULLETIN] Yeni ilan: ${post.post_id} - Gönderen: ${post.sender_name}`);

    try {
        const bulletinData = {
            sender_id: post.sender_id,
            sender_name: post.sender_name,
            content: post.content,
            type: post.type,
            location: new admin.firestore.GeoPoint(Number(post.latitude) || 0, Number(post.longitude) || 0),
            original_timestamp: admin.firestore.Timestamp.fromMillis(Number(post.timestamp) || Date.now()),
            cloud_timestamp: admin.firestore.FieldValue.serverTimestamp(),
            ttl: Number(post.ttl) || 10
        };

        await db.collection('bulletins').doc(post.post_id).set(bulletinData);
        console.log(`✅ İlan Firestore'a yazıldı: ${post.post_id}`);
        callback(null, { success: true, message: "İlan başarıyla senkronize edildi." });
    } catch (error) {
        console.error(`❌ İlan (${post.post_id}) Firestore yazma hatası:`, error);
        callback(null, { success: false, message: `Sunucu Yazma Hatası: ${error.message}` });
    }
}

// C:/Users/bedir/AndroidStudioProjects/YANKI-Decentralized-Mesh-Network/backend-server/server.js

// Change the function to handle a single call instead of events
async function SyncMessages(call, callback) {
    const message = call.request;
    console.log(`[SYNC] Mesaj alınıyor: ${message.msg_id} - Kimden: ${message.sender_id}`);

    try {
        if (!message.msg_id || !message.sender_id) {
            throw new Error("Eksik veri: msg_id veya sender_id bulunamadı.");
        }

        // gRPC bytes -> Buffer -> String dönüşümü (Eğer içerik metinse)
        let contentStr = "";
        if (message.content_blob && message.content_blob.length > 0) {
            contentStr = Buffer.from(message.content_blob).toString('utf8');
        } else {
            contentStr = "[Boş Mesaj]";
        }

        const msgData = {
            sender_id: message.sender_id,
            sender_name: message.sender_name || "Bilinmiyor",
            receiver_id: message.receiver_id || "Herkes",
            content_text: contentStr,
            content_raw: message.content_blob,
            original_timestamp: admin.firestore.Timestamp.fromMillis(Number(message.timestamp) || Date.now()),
            cloud_timestamp: admin.firestore.FieldValue.serverTimestamp(),
            status: Number(message.status) || 0,
            is_synced: true,
            ttl: Number(message.ttl) || 7
        };

        await db.collection('messages').doc(message.msg_id).set(msgData);

        console.log(`✅ Mesaj Firestore'a yazıldı: ${message.msg_id} -> ${contentStr}`);
        callback(null, { success: true, message: "Mesaj başarıyla senkronize edildi." });
    } catch (error) {
        console.error(`❌ Mesaj (${message.msg_id}) Firestore yazma hatası:`, error);
        // Hata durumunda istemciye detaylı mesaj dön
        callback(null, { success: false, message: `Sunucu Yazma Hatası: ${error.message}` });
    }
}

async function GetNewData(call, callback) {
    const { user_id, last_sync_time } = call.request;
    console.log(`[PULL] Veri talebi: ${user_id} - Son Senk: ${last_sync_time}`);

    try {
        // Son senkronizasyondan sonra eklenen mesajları getir
        const messagesSnapshot = await db.collection('messages')
            .where('original_timestamp', '>', admin.firestore.Timestamp.fromMillis(parseInt(last_sync_time)))
            .limit(50)
            .get();

        const signalsSnapshot = await db.collection('emergency_signals')
            .where('created_at', '>', admin.firestore.Timestamp.fromMillis(parseInt(last_sync_time)))
            .limit(10)
            .get();

        const bulletinsSnapshot = await db.collection('bulletins')
            .where('original_timestamp', '>', admin.firestore.Timestamp.fromMillis(parseInt(last_sync_time)))
            .limit(20)
            .get();

        const messages = messagesSnapshot.docs.map(doc => {
            const data = doc.data();
            return {
                msg_id: doc.id,
                sender_id: data.sender_id,
                sender_name: data.sender_name || "",
                receiver_id: data.receiver_id,
                content_blob: data.content_raw ? data.content_raw.buffer : Buffer.from(data.content_text),
                timestamp: data.original_timestamp.toMillis().toString(),
                status: data.status,
                ttl: data.ttl
            };
        });

        const signals = signalsSnapshot.docs.map(doc => {
            const data = doc.data();
            return {
                signal_id: doc.id,
                user_id: data.user_id,
                user_name: data.user_name || "",
                latitude: data.location.latitude,
                longitude: data.location.longitude,
                emergency_type: data.emergency_type,
                battery_level: data.battery_level,
                hop_count: data.mesh_hops,
                timestamp: data.created_at.toMillis().toString()
            };
        });

        const bulletins = bulletinsSnapshot.docs.map(doc => {
            const data = doc.data();
            return {
                post_id: doc.id,
                sender_id: data.sender_id,
                sender_name: data.sender_name,
                content: data.content,
                timestamp: data.original_timestamp.toMillis().toString(),
                type: data.type,
                latitude: data.location.latitude,
                longitude: data.location.longitude,
                ttl: data.ttl
            };
        });

        callback(null, { messages, signals, bulletins });
    } catch (error) {
        console.error("PULL Hatası:", error);
        callback(null, { messages: [], signals: [], bulletins: [] });
    }
}

// --- Web Paneli (HTTP Sunucu) ---
function startWebServer() {
    const WEB_DIR    = path.join(__dirname, '..', 'web');
    const INDEX_HTML = path.join(WEB_DIR, 'index.html');
    const WEB_PORT   = process.env.WEB_PORT || 3000;

    const MIME_TYPES = {
        '.html': 'text/html; charset=utf-8',
        '.js':   'application/javascript',
        '.css':  'text/css',
        '.png':  'image/png',
        '.jpg':  'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif':  'image/gif',
        '.svg':  'image/svg+xml',
        '.ico':  'image/x-icon',
    };

    http.createServer((req, res) => {
        const urlPath  = req.url === '/' ? '/index.html' : req.url.split('?')[0];
        const filePath = path.resolve(WEB_DIR, '.' + urlPath);

        if (!filePath.startsWith(WEB_DIR)) {
            res.writeHead(403);
            res.end('Forbidden');
            return;
        }

        fs.readFile(filePath, (err, data) => {
            if (err) {
                fs.readFile(INDEX_HTML, (err2, data2) => {
                    if (err2) {
                        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
                        res.end('Web paneli yüklenemedi: ' + err2.message);
                        return;
                    }
                    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
                    res.end(data2);
                });
                return;
            }
            const ext = path.extname(filePath).toLowerCase();
            const contentType = MIME_TYPES[ext] || 'application/octet-stream';
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(data);
        });
    }).listen(WEB_PORT, () => {
        console.log(`🌐 Web Paneli  : http://localhost:${WEB_PORT}`);
    });
}

// --- gRPC + Web Sunucu Başlatma ---
function main() {
    const server = new grpc.Server();
    server.addService(yankiProto.YankiSyncService.service, {
        RegisterUser: RegisterUser,
        SendEmergencySignal: SendEmergencySignal,
        SyncMessages: SyncMessages,
        SyncBulletin: SyncBulletin,
        GetNewData: GetNewData
    });

    const HOST_PORT = '0.0.0.0:50051';
    server.bindAsync(HOST_PORT, grpc.ServerCredentials.createInsecure(), (error, port) => {
        if (error) {
            console.error("Sunucu hatası:", error);
            return;
        }
        console.log(`🚀 YANKI Kontrol Merkezi Aktif!`);
        console.log(`📡 gRPC Adresi : ${HOST_PORT}`);
    });

    startWebServer();

    // Her 1 saatte bir eski verileri temizle (48 saat sınırı)
    setInterval(cleanupOldData, 60 * 60 * 1000);
    // İlk açılışta da bir kez çalıştır
    cleanupOldData();
}

async function cleanupOldData() {
    const threshold = Date.now() - (48 * 60 * 60 * 1000);
    const thresholdDate = admin.firestore.Timestamp.fromMillis(threshold);

    console.log(`[CLEANUP] 48 saatten eski veriler temizleniyor... (Eşik: ${new Date(threshold).toLocaleString()})`);

    try {
        // Eski SOS sinyallerini bul ve sil
        const oldSignals = await db.collection('emergency_signals')
            .where('created_at', '<', thresholdDate)
            .get();

        if (!oldSignals.empty) {
            const batch = db.batch();
            oldSignals.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
            console.log(`[CLEANUP] ${oldSignals.size} adet eski SOS sinyali silindi.`);
        }

        // Eski ilanları bul ve sil
        const oldBulletins = await db.collection('bulletins')
            .where('original_timestamp', '<', thresholdDate)
            .get();

        if (!oldBulletins.empty) {
            const batch = db.batch();
            oldBulletins.forEach(doc => batch.delete(doc.ref));
            await batch.commit();
            console.log(`[CLEANUP] ${oldBulletins.size} adet eski ilan silindi.`);
        }
    } catch (error) {
        console.error("[CLEANUP] Hata:", error);
    }
}

main();