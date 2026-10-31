# Firebase Cloud Sync, Error Reporting & Feedback

Integrates Firebase Firestore and Google Sign-In into GlidePad, enabling cloud synchronization for custom macro decks and PC profiles, alongside user feedback submission and diagnostic error reporting directly to the cloud.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The implementation plan has been updated to incorporate user feedback and error reporting provisions:

* **Error Reporting & Diagnostics**: Adds a cloud error-reporting channel (`/reports/{reportId}`) allowing users to submit connection diagnostics, stack traces, and network logs directly to Firestore when troubleshooting Wi-Fi or Bluetooth pairing.
* **User Suggestions & Feature Requests**: Adds an in-app feedback modal and Firestore collection (`/feedback/{feedbackId}`) where users can suggest new gestures, macros, or improvements.
* **UI Integration**: Adds dedicated **"Send Feedback"** and **"Report Issue"** dialogs in the Settings tab, complete with category selection, log attachment toggles, and status toasts.
* **Confirmed Data Scope**: Synchronizes custom macro decks, saved PC profiles, user feedback, and diagnostic error logs.
* **Backend Parameters**:
  * Project ID: `gen-lang-client-0572362862`
  * Region: `us-west2`
  * Target Product: Firestore (`firebaseProduct: 1`)

---

### 1. Overview & Core Concept

* **What It Does**: Equips GlidePad with Google Cloud Firestore and Firebase Authentication to provide three core cloud capabilities:
  1. **Profile & Macro Sync**: Cross-device synchronization of user-created macros, sensitivity preferences, and multi-PC connection settings.
  2. **Error & Diagnostic Reporting**: One-tap diagnostic log submission to help troubleshoot UDP/Wi-Fi or Bluetooth host compatibility.
  3. **User Suggestions**: Direct in-app feature request and feedback pipeline to submit ideas for new gestures or tools.
* **Target Audience**: Power users, multi-device owners, and beta testers who want synchronized setups and a direct voice in app evolution.
* **Key Value**: Preserves customized shortcuts, simplifies cross-device setups, and provides observability for network troubleshooting.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Cloud Sync & Account**:
   * Located in Settings under "Cloud Sync & Account".
   * Unauthenticated state shows a "Sign in with Google" button.
   * Authenticated state displays user avatar, email, and live cloud sync indicator.
2. **Submitting a Suggestion / Feature Request**:
   * In Settings $\rightarrow$ "Support & Feedback", tap **"Suggest a Feature"**.
   * A Compose dialog prompts for Category (New Gesture, Macro, Wi-Fi Tool, Keyboard, Other), Title, and Details.
   * Tapping **"Submit Suggestion"** writes to Firestore with a success confirmation toast.
3. **Reporting a Connection Error**:
   * If a connection fails or from the Support section, tap **"Report Issue / Send Logs"**.
   * Pre-populates app version, connection mode, and recent diagnostic logs (e.g. UDP timeout, packet drops).
   * User can add optional comments and submit the report to `/reports`.
4. **Offline Handling**:
   * Feedback and reports submitted while offline queue automatically in Firestore's offline cache and upload once connectivity resumes.

#### Visual Styling
* **Card & Dialog Styling**: Follows the dark cyber-slate palette (`SurfaceDark800` card surfaces, `NeonCyan` accents, `ElectricBlue` buttons).
* **Feedback Category Chips**: Selectable pill filters for fast suggestion categorizing.
* **Log Preview Box**: Monospaced terminal preview of the diagnostic snapshot being submitted with privacy disclosure.

---

### 3. Key Product Decisions & Trade-Offs

* **Decision 1: Direct Firestore Submission vs. Third-Party Crash SDKs**
  * *Chosen Approach*: Collect feedback and diagnostic reports directly in dedicated Firestore collections (`/feedback` and `/reports`).
  * *Why*: Keeps the app lightweight and free of heavy ad-adjacent telemetry SDKs while giving full ownership of user suggestions and diagnostic logs.
* **Decision 2: User-Controlled Log Submission**
  * *Chosen Approach*: Explicit user-initiated issue reporting with an inspectable log preview before sending.
  * *Why*: Protects user privacy by ensuring network addresses and logs are only transmitted when the user explicitly requests support.
* **Decision 3: Zero-Trust Security Rules for Feedback & Reports**
  * *Chosen Approach*: Authenticated write-only rules (`create: if request.auth != null`) with strict schema validation (`isValidFeedback()`, `isValidReport()`). Read access is denied to regular clients to ensure privacy.

---

### 4. Technical Architecture & Data Strategy

```
 ┌─────────────────────────────────────────────────────────────┐
 │                         GlidePad UI                         │
 │  ┌───────────────────────┐   ┌───────────────────────────┐  │
 │  │ Settings: Cloud Sync  │   │  Feedback & Issue Modals  │  │
 │  └───────────┬───────────┘   └─────────────┬─────────────┘  │
 └──────────────┼─────────────────────────────┼────────────────┘
                │                             │
 ┌──────────────▼─────────────────────────────▼────────────────┐
 │                      MainViewModel                          │
 │  ┌───────────────────────┐   ┌───────────────────────────┐  │
 │  │ CredentialManager Auth│   │  Feedback & Report State  │  │
 │  └───────────┬───────────┘   └─────────────┬─────────────┘  │
 └──────────────┼─────────────────────────────┼────────────────┘
                │                             │
 ┌──────────────▼─────────────────────────────▼────────────────┐
 │                 Firebase Firestore Repositories             │
 │  - CloudMacrosRepository     - FeedbackRepository           │
 │  - PcProfilesRepository      - ErrorReportsRepository       │
 └──────────────────────────────┬──────────────────────────────┘
                                │
 ┌──────────────────────────────▼──────────────────────────────┐
 │                 Google Cloud Firestore (us-west2)           │
 │  /users/{uid}                                               │
 │    ├── /macros/{id}            (Custom Hotkey Decks)        │
 │    └── /pc_profiles/{id}       (Saved PC IPs & MACs)        │
 │  /feedback/{id}                (User Suggestions)           │
 │  /reports/{id}                 (Diagnostic Error Reports)   │
 └─────────────────────────────────────────────────────────────┘
```

#### Firestore Data Models

##### 1. `UserFeedback` (Collection: `/feedback/{id}`)
```kotlin
data class UserFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val category: String = "FEATURE", // FEATURE, GESTURE, WIFI, OTHER
    val title: String = "",
    val description: String = "",
    val appVersion: String = "4.0",
    @ServerTimestamp val createdAt: Timestamp? = null
)
```

##### 2. `ErrorReport` (Collection: `/reports/{id}`)
```kotlin
data class ErrorReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val connectionMode: String = "LOCAL_WIFI",
    val targetIp: String = "",
    val errorMessage: String = "",
    val userComments: String = "",
    val recentLogs: List<String> = emptyList(),
    val appVersion: String = "4.0",
    val osVersion: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
```

##### 3. `CloudMacro` (Collection: `/users/{uid}/macros/{id}`)
```kotlin
data class CloudMacro(
    @DocumentId val id: String = "",
    val label: String = "",
    val description: String = "",
    val shortcutKeys: List<String> = emptyList(),
    val actionType: String = "KEYBOARD",
    val targetPayload: String = "",
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
```

##### 4. `SavedPcProfile` (Collection: `/users/{uid}/pc_profiles/{id}`)
```kotlin
data class SavedPcProfile(
    @DocumentId val id: String = "",
    val name: String = "Home PC",
    val ipAddress: String = "192.168.1.100",
    val port: Int = 8089,
    val macAddress: String = "",
    val isDefault: Boolean = false,
    val pointerSensitivity: Float = 1.25f,
    val accelerationMultiplier: Float = 1.20f,
    val accelerationProfile: String = "DYNAMIC",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
```

---

### 5. Implementation Steps (Upon Approval)

1. **Keystore Extraction**: Run `keytool` on `debug.keystore` to extract SHA-1 and SHA-256 fingerprints.
2. **Backend Provisioning**: Call `ProvisionFirebase` via `rpc_action` targeting project `gen-lang-client-0572362862`, region `us-west2`, and product `["firestore"]`.
3. **Verify Fingerprint**: Ensure `google-services.json` contains the registered signing certificate hash.
4. **Dependencies**: Enable pre-configured Firebase Firestore and Auth artifacts in `app/build.gradle.kts`.
5. **Security Rules & Testing**:
   * Generate `firebase-blueprint.json` and `firestore.rules` supporting `/users/{uid}/*`, `/feedback/*`, and `/reports/*`.
   * Configure `firebase.json` for local Firestore emulator testing.
   * Write and execute JS and Kotlin rule integration tests.
   * Deploy rules using `DeployRules` via `rpc_action`.
6. **Data Repositories & UI**:
   * Implement `FeedbackRepository` and `ErrorReportsRepository`.
   * Add Google Sign-In button and session observer in Settings.
   * Add interactive Compose dialogs for "Suggest a Feature" and "Report an Issue" with diagnostic log attachments.
   * Verify compilation and tests.
