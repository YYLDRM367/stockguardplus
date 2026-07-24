import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import {
  createUserWithEmailAndPassword,
  EmailAuthProvider,
  onAuthStateChanged,
  reauthenticateWithCredential,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User
} from "firebase/auth";
import { collection, doc, getDocs, writeBatch, type CollectionReference } from "firebase/firestore";
import { auth, db } from "../firebase";

interface AuthContextValue {
  user: User | null;
  orgId: string | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string, businessName: string) => Promise<void>;
  signOut: () => Promise<void>;
  deleteAccount: (password: string) => Promise<void>;
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

  async function deleteCollection(collectionRef: CollectionReference) {
    const snapshot = await getDocs(collectionRef);
    if (snapshot.empty) return;
    const docs = snapshot.docs;
    for (let i = 0; i < docs.length; i += 450) {
      const batch = writeBatch(db);
      docs.slice(i, i + 450).forEach((d) => batch.delete(d.ref));
      await batch.commit();
    }
  }

  // Mirrors FirebaseAuthRepository.deleteAccount on Android exactly:
  // re-authenticate, delete every Firestore collection under the org in
  // batches, delete the org doc, then delete the Auth user itself.
  async function deleteAccount(password: string) {
    const current = auth.currentUser;
    if (!current || !current.email) throw new Error("No signed-in user.");

    const credential = EmailAuthProvider.credential(current.email, password);
    await reauthenticateWithCredential(current, credential);

    const orgId = current.uid;
    const orgRef = doc(db, "organizations", orgId);

    await deleteCollection(collection(orgRef, "products"));
    await deleteCollection(collection(orgRef, "categories"));
    await deleteCollection(collection(orgRef, "parties"));
    await deleteCollection(collection(orgRef, "orders"));
    await deleteCollection(collection(orgRef, "movements"));
    await deleteCollection(collection(orgRef, "members"));

    const batch = writeBatch(db);
    batch.delete(orgRef);
    await batch.commit();

    await current.delete();
  }

  const value: AuthContextValue = {
    user,
    orgId: user?.uid ?? null,
    loading,
    signIn,
    signUp,
    signOut,
    deleteAccount
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
