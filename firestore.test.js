const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const DB_ID = "ai-studio-android-glidepad-c3cd9a98-fc75-4ebc-a03b-2c7aedc8e39c";

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: "gen-lang-client-0572362862",
    firestore: {
      rules: rules,
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("authenticated user can create their own macro", async () => {
  const alice = testEnv.authenticatedContext("alice_uid");
  const db = alice.firestore({ databaseId: DB_ID });
  const macroRef = db.collection("users/alice_uid/macros").doc("macro_1");

  await assertSucceeds(macroRef.set({
    label: "Copy",
    description: "Ctrl+C",
    shortcutKeys: ["Ctrl", "C"],
    actionType: "KEYBOARD",
    sortOrder: 1
  }));
});

test("unauthenticated user cannot create or read macros", async () => {
  const anon = testEnv.unauthenticatedContext();
  const db = anon.firestore({ databaseId: DB_ID });
  const macroRef = db.collection("users/alice_uid/macros").doc("macro_1");

  await assertFails(macroRef.get());
  await assertFails(macroRef.set({
    label: "Copy",
    actionType: "KEYBOARD"
  }));
});

test("bob cannot read alice's private macros", async () => {
  const bob = testEnv.authenticatedContext("bob_uid");
  const db = bob.firestore({ databaseId: DB_ID });
  const macroRef = db.collection("users/alice_uid/macros").doc("macro_1");

  await assertFails(macroRef.get());
});

test("authenticated user can submit feedback but cannot read others", async () => {
  const alice = testEnv.authenticatedContext("alice_uid");
  const db = alice.firestore({ databaseId: DB_ID });
  const feedbackRef = db.collection("feedback").doc("fb_1");

  await assertSucceeds(feedbackRef.set({
    userId: "alice_uid",
    title: "Add Gesture",
    description: "Would love a 5-finger pinch to sleep PC.",
    category: "FEATURE"
  }));

  // Read should fail as feedback is write-only for users
  await assertFails(feedbackRef.get());
});

test("authenticated user can submit diagnostic error report", async () => {
  const alice = testEnv.authenticatedContext("alice_uid");
  const db = alice.firestore({ databaseId: DB_ID });
  const reportRef = db.collection("reports").doc("rep_1");

  await assertSucceeds(reportRef.set({
    userId: "alice_uid",
    errorMessage: "UDP connection timed out",
    connectionMode: "LOCAL_WIFI",
    targetIp: "192.168.1.100"
  }));

  // Read should fail
  await assertFails(reportRef.get());
});
