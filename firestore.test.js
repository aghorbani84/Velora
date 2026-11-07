const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user profiles or playlists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cloud_playlists").get());
});

test("Authenticated user: can create, read and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    aliceDoc.set({
      userId: ALICE_UID,
      displayName: "Alice Audiophile",
      email: "alice@example.com",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDoc.get());

  await assertSucceeds(
    aliceDoc.update({
      displayName: "Alice Golden Ears",
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access denied: Alice cannot read or write Bob's playlists", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("cloud_playlists").doc("bobs_mix").set({
      userId: BOB_UID,
      name: "Bob's Heavy Metal",
      trackIds: ["track_1", "track_2"],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("cloud_playlists").doc("bobs_mix").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("cloud_playlists").doc("bobs_mix").delete()
  );
});

test("Authenticated user: can create and read their own cloud EQ presets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const presetDoc = aliceDb.collection("users").doc(ALICE_UID).collection("cloud_eq_presets").doc("warm_vinyl");

  await assertSucceeds(
    presetDoc.set({
      userId: ALICE_UID,
      name: "Warm Vinyl Master",
      gainsDb: [4.0, 3.0, 1.5, 0.0, -1.0, 0.0, 1.5, 2.5, 3.5, 4.0],
      createdAt: new Date(),
    })
  );

  await assertSucceeds(presetDoc.get());
});
