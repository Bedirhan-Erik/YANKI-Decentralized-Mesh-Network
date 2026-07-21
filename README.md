<div align="center">

# 📡 YANKI

### Afet Senaryolarında Merkeziyetsiz Mesh Ağ Tabanlı Mobil Haberleşme Sistemi

Hücresel altyapının çöktüğü afet anlarında, internet olmadan cihazdan cihaza haberleşme.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=node.js&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

</div>

---

## 🎯 Proje Hakkında

**YANKI**, doğal afetlerde baz istasyonları ve fiber hatlar devre dışı kaldığında, mobil cihazlar arasında **merkeziyetsiz (decentralized)** iletişim sağlamak için geliştirilmiş bir mesh ağ sistemidir. 2023 Kahramanmaraş depremlerinde yaşanan haberleşme kesintileri, bu projenin çıkış noktasını oluşturmuştur.

Uygulama; **Bluetooth Low Energy (BLE)** ve **Wi-Fi Aware (NAN)** teknolojilerini hibrit olarak kullanarak Android cihazlar arasında bir mesh ağ topolojisi kurar. Böylece internet bağlantısı olmadan mesajlaşma, acil durum sinyali yayını, ilan tahtası ve konum paylaşımı mümkün hale gelir. Ağdaki herhangi bir cihaz internete eriştiğinde, toplanan tüm veriler **gRPC/Protobuf** üzerinden **Firebase** bulut altyapısına senkronize edilir.

> Ege Üniversitesi Ege Meslek Yüksekokulu Bilgisayar Programcılığı bitirme projesi olarak geliştirilmiştir.

## ✨ Temel Özellikler

- **İnternetsiz haberleşme:** BLE ve Wi-Fi Aware ile cihazdan cihaza mesh ağ.
- **Uçtan uca şifreli mesajlaşma:** Ed25519 dijital imza + AES-GCM şifreleme.
- **Acil durum sinyali (SOS):** Deprem, sel, yangın gibi afet türleriyle mesh ağına yayın.
- **İlan tahtası (Duyuru Panosu):** Ağdaki kullanıcıların gördüğü ortak duyurular.
- **Komşu haritası:** Yakındaki cihazların canlı olarak görselleştirilmesi.
- **Sağlık bilgileri:** Kan grubu, alerjiler, ilaçlar ve acil durum yakını — kurtarma ekipleri için.
- **Hibrit bulut senkronizasyonu:** İnternet erişimi olan düğüm, verileri Firebase'e aktarır.
- **Gossip protokolü:** TTL/hop tabanlı, merkezi koordinasyon gerektirmeyen veri yayılımı.

## 🏗️ Sistem Mimarisi

YANKI üç katmanlı bir mimari üzerine kuruludur:

1. **Cihaz İçi Katman** — MVVM tasarım deseni, Jetpack Compose ile reaktif arayüz, Hilt ile bağımlılık enjeksiyonu, Room ile yerel veri yönetimi.
2. **Haberleşme Katmanı** — BLE (cihaz keşfi ve küçük veri; 128-bit UUID, GATT, 480 baytlık parçalama) ve Wi-Fi Aware (yüksek kapasiteli transfer; Publish/Subscribe, PSK'li TCP soket).
3. **Bulut Senkronizasyon Katmanı** — gRPC/Protobuf üzerinden Firebase Firestore'a çift yönlü (push/pull) senkronizasyon.

Ağ üzerindeki veri yayılımı **epidemik (gossip) protokolü** ile sağlanır; her mesajdaki TTL alanı hop sayısını sınırlar, mükerrer mesajlar UUID ile filtrelenir. Tüm veri alışverişi Protobuf formatındadır (JSON'a kıyasla ~%70 daha küçük paket boyutu).

## 🛠️ Kullanılan Teknolojiler

**Mobil (Android)**
- Kotlin, Jetpack Compose (deklaratif UI)
- Dagger Hilt (bağımlılık enjeksiyonu)
- Room + Kotlin Coroutines & Flow (asenkron yerel veritabanı)
- Android WorkManager (arka plan senkronizasyonu)
- BLE (GATT) & Wi-Fi Aware (NAN)

**Güvenlik**
- Ed25519 asimetrik imza
- AES-GCM şifreleme (Google Tink)
- Android Keystore (donanım güvenlik modülü)

**Sunucu & Bulut**
- Node.js + gRPC
- Firebase Firestore
- Protocol Buffers (Protobuf)

**Web Panel**
- HTML/JavaScript, Leaflet.js (canlı harita paneli)

## 🧩 Temel Bileşenler

| Bileşen | Görevi |
|---|---|
| `BleMeshManager` | BLE tarama, yayın ve GATT sunucu/istemci; parçalama ve birleştirme. |
| `WifiAwareManager` | Publish/Subscribe keşfi, PSK tabanlı bağlantı ve soket veri iletimi. |
| `YankiRepository` | Merkezi veri akışı; mesh verisini yerele kaydeder, gossip ve senkronizasyonu tetikler. |
| `SyncWorker` | İnternet gelince gRPC ile buluta çift yönlü veri aktarımı. |
| `MeshService` | Ön plan servisi; BLE ve Wi-Fi Aware oturumlarını yönetir. |

## 🚀 Gelecek Çalışmalar

- Detaylı saha testleri (menzil, gecikme, paket kaybı ölçümleri)
- iOS platformu desteği
- Dosya ve görsel paylaşımı
- Harita tabanlı komşu görselleştirmesi
- Enerji tüketimi optimizasyonu

## 👥 Ekip

Bu proje **Ege Üniversitesi Ege Meslek Yüksekokulu Bilgisayar Programcılığı** öğrencileri tarafından geliştirilmiştir.

| Geliştirici | GitHub | LinkedIn |
|---|---|---|
| **Bedirhan Erik** | [@Bedirhan-Erik](https://github.com/Bedirhan-Erik) | [LinkedIn](https://www.linkedin.com/in/bedirhan-erik-842295228/) |
| **Fatih Polat** | [@fatihspy](https://github.com/fatihspy) | [LinkedIn](https://www.linkedin.com/in/fatih-polat-5069a6289/) |
| **Ahmet Özer** | [@ysfdmrhn](https://github.com/ysfdmrhn) | — |
| **Yusuf Demirhan** | — | [LinkedIn](https://www.linkedin.com/in/ahmet-%C3%B6zer-698b3a283/) |

## 📄 Lisans

Bu proje MIT lisansı altında sunulmaktadır.
