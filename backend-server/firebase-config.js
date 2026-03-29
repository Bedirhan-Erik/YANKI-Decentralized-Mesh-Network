const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

// Firebase Admin SDK'yı başlatıyoruz
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Firestore veritabanı referansını alıyoruz
const db = admin.firestore();

module.exports = { db };