import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

// Same Firebase project as the Android app — this is a public client config
// (not a secret; access is governed by Firestore security rules), so it's
// safe to commit directly rather than gitignore it like google-services.json.
const firebaseConfig = {
  apiKey: "AIzaSyBhWI1Bb5x8IB1Q77OqaUi81A9u-aEUgtM",
  authDomain: "stockguardplus.firebaseapp.com",
  projectId: "stockguardplus",
  storageBucket: "stockguardplus.firebasestorage.app",
  messagingSenderId: "956067438094",
  appId: "1:956067438094:web:466655e498986778a5665f"
};

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const db = getFirestore(firebaseApp);
