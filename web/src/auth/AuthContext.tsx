import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User
} from "firebase/auth";
import { doc, writeBatch } from "firebase/firestore";
import { auth, db } from "../firebase";

interface AuthContextValue {
  user: User | null;
  orgId: string | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string, businessName: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
      setUser(firebaseUser);
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  async function signIn(email: string, password: string) {
    await signInWithEmailAndPassword(auth, email, password);
  }

  // Mirrors FirebaseAuthRepository.signUp on Android exactly: same
  // organizations/{uid} + members/{uid} shape, written in one batch, so an
  // account created on web looks identical to one created on mobile.
  async function signUp(email: string, password: string, businessName: string) {
    const credential = await createUserWithEmailAndPassword(auth, email, password);
    const uid = credential.user.uid;

    const batch = writeBatch(db);
    batch.set(doc(db, "organizations", uid), {
      name: businessName,
      language: navigator.language.split("-")[0] || "en",
      subscriptionPlan: "free",
      subscriptionExpiry: null
    });
    batch.set(doc(db, "organizations", uid, "members", uid), { role: "owner" });
    await batch.commit();
  }

  async function signOut() {
    await firebaseSignOut(auth);
  }

  const value: AuthContextValue = {
    user,
    orgId: user?.uid ?? null,
    loading,
    signIn,
    signUp,
    signOut
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
