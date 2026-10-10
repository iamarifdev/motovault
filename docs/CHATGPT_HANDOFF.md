# MotoVault — Complete Product & Engineering Handoff for ChatGPT

> **Instructions for ChatGPT:**  
> You are continuing development on **MotoVault** as the Principal Kotlin Multiplatform (KMP) Architect and Senior Automotive UI/UX Engineer. Read this entire document carefully. It contains the complete context, history, design decisions, domain logic, codebase layout, and future roadmap. Do not make assumptions that contradict this document.

---

## 1. Project Overview & Genesis

### 1.1 What is MotoVault?
**MotoVault** is an offline-first vehicle digital passport, maintenance vault, and lifecycle health tracker engineered as a luxury automotive companion. It is built using **Kotlin Multiplatform (KMP 2.0.21)** and **Compose Multiplatform (1.7.0)** targeting both **Android** and **iOS**.

### 1.2 Who is the User & What is the Origin Story?
* **User Profile:** A passionate vehicle owner and software engineer based in Dhaka, Bangladesh (`iamarifdev` / Md. Arifur Rahman).
* **Initial Vehicle:** **KTM Duke 125 Indian Version** (manufactured at Bajaj Auto's Chakan plant in Pune, India).
* **The Core Frustrations MotoVault Solves:**
  1. **Bangladeshi Document Compliance:** In Bangladesh, riders must legally present three separate documents at police checkpoints:
     * **BRTA Registration Smart Card** (Digital Chip Smart Card)
     * **BRTA Driving Licence Card** (Plastic Smart Card with blood group & authorized classes)
     * **BRTA Motor Vehicle Tax Token Certificate** (Annual / multi-year tax slip)
     Paper cards get worn, lost, or forgotten at home. Generic digital wallets don't look like authentic Bangladeshi cards or work well during tense roadside police inspections.
  2. **Motorcycle-Specific Maintenance Neglect:** High-revving motorcycles need frequent care that car apps don't handle (e.g., chain lubing every 500 km, strict 10W-50 oil grades, valve clearance).
  3. **Resale Value Depletion:** In Bangladesh, secondary vehicle sales suffer from disputed paper service receipts. MotoVault generates a verified, exportable digital service passport.
  4. **Design Quality Bar:** The user has an exceptionally high bar for UI/UX. Clunky, flat, generic Material-style forms were explicitly rejected. The app must feel like a luxury automotive cockpit (Rivian + Apple Wallet + Bosch TFT).

---

## 2. Motorcycle Domain Knowledge: KTM Duke 125

The app encodes factory manufacturer specifications for the KTM Duke 125. The emission standard is **strictly coupled** to the regional market edition:

```mermaid
flowchart TD
    Edition{"Market Edition"}
    Edition -->|Indian Edition (Bajaj Chakan)| IndBranch["Indian Standards:\n• BS4 (2018–early 2020)\n• BS6 / OBD-2 (2020–present)"]
    Edition -->|European Edition (EU / Euro 5)| EuBranch["European Standards:\n• Euro 4 (2017–2020)\n• Euro 5 / 5+ (2021–present)"]
```

### 2.1 Critical Mechanical & Specification Differences

| Feature | Indian Edition (BS4) | Indian Edition (BS6) | European Edition (Euro 5) |
| :--- | :--- | :--- | :--- |
| **Years** | 2018 – early 2020 | 2020 – present | 2021 – present |
| **Fuel Tank Capacity** | **10.2 Liters** | **13.5 Liters** | **13.4 Liters** |
| **Chassis / Frame** | Continuous steel trellis | **Split Trellis Frame** (bolt-on subframe) | Split Trellis Frame |
| **Exhaust Design** | Underbelly exhaust chamber | Side-slung upswept exhaust canister | Side-slung with twin catalytic converters |
| **Engine Specs** | 124.7cc single-cylinder DOHC, 4-valve, liquid-cooled, fuel-injected | 124.7cc single-cylinder DOHC, 4-valve, liquid-cooled, fuel-injected | 124.7cc single-cylinder DOHC, 4-valve, liquid-cooled, fuel-injected |
| **Power / Torque** | 14.5 PS @ 9,250 RPM / 12 Nm | 14.5 PS @ 9,250 RPM / 12 Nm | 15.0 PS @ 10,000 RPM / 12 Nm |
| **Factory Oil Spec** | Motul 7100 10W-50 JASO MA2 (1.5L) | Motul 7100 10W-50 JASO MA2 (1.5L) | Motorex Formula 4T 15W-50 (1.5L) |
| **Chain Specification** | 520 O-Ring (5–7 mm slack) | 520 X-Ring (5–7 mm slack) | 520 X-Ring (5–7 mm slack) |
| **Braking System** | ByBre (Brembo subsidiary) 300mm front / 230mm rear disc, Single-channel Bosch ABS | ByBre 300mm front / 230mm rear disc, Single-channel Bosch ABS | ByBre 300mm front / 230mm rear disc, Dual-channel Bosch ABS |

---

## 3. Mathematical Engines & Core Domain Logic

### 3.1 Dual-Threshold Component Wear Math
Components degrade through both mechanical usage ($\text{km}$) and chemical/temporal aging ($\text{days}$). The system evaluates both and takes the higher of the two:

$$\text{Wear Percentage} = \max\left( \frac{\text{Current Odo} - \text{Last Service Odo}}{\text{Interval}_{\text{km}}}, \; \frac{\text{Current Days} - \text{Last Service Days}}{\text{Interval}_{\text{days}}} \right) \times 100$$

* **Health Classification:**
  * $< 75\%$: **Healthy** (`#10B981` Emerald Green)
  * $75\% \text{ to } 99\%$: **Service Soon** (`#F59E0B` Amber Gold)
  * $\ge 100\%$: **Overdue** (`#EF4444` Crimson Red)
* **Defensive Edge Cases Built-In:**
  * Odometer rollback / typo protection: If $\text{Current Odo} < \text{Last Service Odo}$, wear returns $0\%$ without arithmetic overflow.
  * Zero-interval defense: Intervals of $0\text{ km}$ or $0\text{ days}$ are guarded against division-by-zero.
  * 1-Tap quick lube reset: Resets chain counter to current odometer with zero friction.

### 3.2 Fuel Efficiency Math (Full-to-Full Telemetry)
$$\text{Efficiency } (\text{km/L}) = \frac{\text{Odometer}_{\text{current}} - \text{Odometer}_{\text{previous}}}{\text{Fuel Liters Added}}$$
* Handles partial fill-ups without corrupting rolling tank averages until the next full tank event.

### 3.3 Regulatory Document Expiry Engine
* Days remaining calculated relative to system epoch.
* $> 60$ days: **Valid / Healthy**
* $\le 60$ days: **Expiring Soon Alert** (amber badge)
* $\le 0$ days: **Expired Urgent Alert** (red badge)

---

## 4. UI/UX Design System Evolution: Automotive Luxury

### 4.1 Why the Initial UI Was Rejected
The initial v1 implementation used flat cards, generic borders, and a long scrolling form. The user rejected it as feeling like a "2015 programmer form" rather than a modern consumer automotive application.

### 4.2 The v2 Luxury Automotive Design Language
We conducted industry research across **Rivian OS 3.0**, **Apple Wallet**, and **Bosch TFT Digital Cockpits** and established:

1. **Canvas & Surfaces:**
   * Pure obsidian dark canvas (`#07080B`).
   * Subtle linear gradient cards (`#161922` to `#111319`).
   * Hairline specular borders (`1px rgba(255, 255, 255, 0.08)`).
   * Inset specular highlights (`1px rgba(255, 255, 255, 0.15)`).
   * Brand apex orange accent (`#FF6600` for KTM).
2. **Onboarding Flow (`VehicleSelectionScreen.kt`):**
   * Apple Setup Assistant 3-Step Paged Flow with animated horizontal slide transitions.
   * Tactile pill-dot indicators (active step is 24.dp wide; inactive are 8.dp dots).
   * Step 1: 2x2 brand grid (KTM active, Yamaha/Ather/Royal Enfield "Soon") + KTM Duke 125 active card.
   * Step 2: Segmented pill switch for Indian vs European edition + dynamic emission cards.
   * Step 3: Tactile digital monospace odometer input with quick chips (`1,000`, `4,200`, `10,000 km`) + factory specs loaded card.
3. **Garage Dashboard (`GarageScreen.kt`):**
   * Vehicle hero stage with ambient radial orange halo spotlight, "READY TO RACE" badge, and precision stats bar.
   * **Bosch TFT Inset Dials (`WearGauge.kt`):** 3-column instrument dials with 36 graduation ticks, specular inset track, glowing progress sweep arcs, and centered monospace readouts (`0% HEALTHY`).
   * Factory Maintenance Schedule checklist with component icons.
   * Floating frosted glass quick action dock (`Lube Chain`, `Fuel Up`, `Police Check`).
4. **Document Vault (`DocumentVaultScreen.kt`):**
   * **Apple Wallet Passbook Vertical Card Stack:** Physical cards overlap vertically with realistic scale and offset interpolation (`spring(dampingRatio = 0.82f, stiffness = 280f)`). Tapping any card springs it forward.
   * **Authentic Bangladeshi Credentials:**
     * **BRTA Registration Smart Card:** Emerald Green (`#0B3323`), BD emblem, gold chip graphic, `DHAKA METRO-HA 45-8821`.
     * **BRTA Driving Licence:** Navy Blue (`#132A4A`), photo avatar, `DK0182949CL`, holder name, blood group B+.
     * **BRTA Tax Token:** Warm Bronze (`#3B2A0F`), countdown `48 Days Remaining`.
   * **3D 180° Card Flip:** Front rotates to back to inspect chassis number (`MD925BK44L1092819`) and engine number (`0-928*88319*`).
   * **Traffic Police Mode:** Instant toggle to **AMOLED Pure Pitch Black (`#000000`)** with maximum-contrast neon borders, emergency header banner, and privacy lockdown of all service/financial logs.

---

## 5. Technical Stack & Repository Structure

* **Git Repository:** `git@github.com:iamarifdev/motovault.git`
* **Default Branch:** `main`
* **Local Workspace:** `/Users/arif/workspace/bbn/motovault`
* **Build System:** Gradle 8.10.2 (using wrapper `./gradlew`)
* **Kotlin:** 2.0.21
* **Compose Multiplatform:** 1.7.0
* **Android Gradle Plugin (AGP):** 8.7.1
* **Target Platforms:** Android (`minSdk = 26`, `targetSdk = 34`) & iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`)
* **JVM Target:** Java 17 (Native Apple Silicon ARM64 OpenJDK 17 at `/opt/homebrew/opt/openjdk@17`)

### 5.1 Project Directory Map

```
composeApp/src/commonMain/kotlin/com/motovault/app/
├── App.kt                             # Root navigation state & dynamic theme wrapper
├── domain/
│   ├── model/
│   │   ├── ComponentRule.kt           # Component wear entity & calculation methods
│   │   ├── GovernmentDocument.kt      # BRTA document entity & validity methods
│   │   ├── Powertrain.kt              # PowertrainType, VehicleCategory, EmissionStandard enums
│   │   └── Vehicle.kt                 # Vehicle entity, Brand enum, VehicleEdition enum
│   ├── preset/
│   │   └── KtmDuke125Preset.kt        # KTM factory rules, capacities, oil specs
│   └── usecase/
│       ├── CalculateWearUseCase.kt    # Dual-threshold wear math engine
│       ├── DocumentExpiryUseCase.kt   # Days-until-expiry calculation engine
│       ├── FuelEfficiencyUseCase.kt   # Tank-to-tank efficiency engine
│       └── LogMaintenanceUseCase.kt   # 1-tap lube reset and service overhaul engine
├── theme/
│   ├── Color.kt                       # Obsidian canvas, specular hairline borders, card gradients
│   ├── Theme.kt                       # MotoVaultTheme & Material3 darkColorScheme
│   └── Type.kt                        # Monospace telemetry typography
└── ui/
    ├── garage/
    │   ├── GarageScreen.kt            # Rivian hero stage, Bosch cluster, maintenance checklist
    │   └── components/
    │       ├── QuickActionBar.kt      # Floating frosted action dock
    │       └── WearGauge.kt           # Bosch TFT dual-ring inset telemetry dial
    ├── onboarding/
    │   └── VehicleSelectionScreen.kt  # Apple Setup Assistant 3-step paged flow
    └── vault/
        ├── DocumentVaultScreen.kt     # Apple Wallet vertical stack & Traffic Police Mode
        └── components/
            └── SkeuomorphicSmartCard.kt # Skeuomorphic card rendering & 3D flip layer
```

---

## 6. Automated Testing Suite & Verification

The codebase has **21 automated unit tests**, all located in `composeApp/src/commonTest/kotlin/com/motovault/app/domain/`:

1. `CalculateWearUseCaseTest.kt` (7 tests): Distance-governed wear, time-governed wear, 100% overdue boundary, zero-interval division safety, odometer rollback defense, status color thresholds.
2. `FuelEfficiencyUseCaseTest.kt` (5 tests): Full-to-full consumption calculations, rolling efficiency averages, partial fill-up handling, zero-volume division safety, negative distance defenses.
3. `DocumentExpiryUseCaseTest.kt` (4 tests): Valid active documents (>60 days), expiring soon alert window ($\le 60$ days), expired documents ($\le 0$ days), negative elapsed days.
4. `KtmDuke125PresetTest.kt` (3 tests): BS4 10.2L vs BS6 13.5L tank capacity, all 7 factory component rules populated, correct 10W-50 oil specifications.
5. `LogMaintenanceUseCaseTest.kt` (2 tests): 1-tap chain lube resets (500 km interval), full service overhaul resets.

### 6.1 Build & Test Verification Commands

```bash
# Set ARM64 JDK 17 explicitly
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Run all 21 unit tests
./gradlew test

# Build Debug APK (~9.6 MB)
./gradlew :composeApp:assembleDebug

# Install and launch on connected Android emulator or device
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n com.motovault.app/.MainActivity

# Capture screenshot for visual inspection
adb exec-out screencap -p > screenshot.png
```

---

## 7. Next Milestones & Extension Playbook for ChatGPT

When asked to implement the next features, here is the exact architectural blueprint to follow:

### Milestone 1: Room KMP Local Persistence
1. Add dependencies to `gradle/libs.versions.toml`:
   * `androidx.room:room-compiler:2.7.0-alpha10`
   * `androidx.room:room-runtime:2.7.0-alpha10`
   * `androidx.sqlite:sqlite-bundled:2.5.0-alpha10`
2. Define SQLite entities in `composeApp/src/commonMain/kotlin/com/motovault/app/data/entity/`:
   * `VehicleEntity` (maps `Vehicle`)
   * `DocumentEntity` (maps `GovernmentDocument`)
   * `MaintenanceLogEntity` (historical log records)
3. Create Room DAOs: `VehicleDao`, `DocumentDao`, `MaintenanceLogDao`.
4. Initialize the database using `bundledSQLiteDriver()` in common KMP code.
5. Inject via Koin so UI states survive application kill and relaunch.

### Milestone 2: Camera Card Scanner (ML Kit OCR)
1. Add CameraX and Google ML Kit Text Recognition:
   * Android: Camera preview overlay with a 85.60 mm × 53.98 mm (ISO ID-1 aspect ratio 1.586) card cutout.
   * iOS: Apple Vision `VNRecognizeTextRequest`.
2. Apply regex filters to OCR text:
   * Registration: `DHAKA METRO-[A-Z]{1,2}\s*\d{2}-\d{4}`
   * Licence No: `[A-Z]{2}\d{9}[A-Z]{2}`
   * Chassis No: `[A-Z0-9]{17}`
   * Tax Token: `TT-\d{6}-\d{4}`
3. Pre-fill scanned data into the `GovernmentDocument` entity for user confirmation.

### Milestone 3: Verified Digital Service Passport (PDF Export)
1. Generate an official PDF report using a multiplatform PDF generator or platform-specific printing adapters.
2. Structure:
   * Vehicle Header: Chassis, Engine, Plate, Owner.
   * Lifetime Maintenance Ledger: Chronological table with Odometer, Date, Service Item, Workshop Name, Invoice Cost.
   * Cryptographic Hash: SHA-256 hash rendered as a verification QR code for prospective buyers to verify ledger authenticity.

### Milestone 4: Supabase Cloud Sync & Backup
1. Add `io.github.jan-tennert.supabase:postgrest-kt` and `gotrue-kt`.
2. Add a `sync_status` column (`PENDING`, `SYNCED`, `CONFLICT`) to Room entities.
3. Sync in background when network is active using Supabase Row Level Security (RLS) scoped to `auth.uid()`.
4. Store encrypted card scan photos in a private Supabase Storage bucket (`user-documents`).

---

## 8. Summary Checklist for ChatGPT
* **Do NOT change the design system:** Retain the deep obsidian canvas (`#07080B`), hairline specular borders, Apple Wallet card stack, and Bosch TFT gauges.
* **Do NOT break domain rules:** Emission standards must stay coupled to vehicle edition; BS4 has 10.2L tank; BS6 has 13.5L tank.
* **Maintain test coverage:** Every new use case or database repository must include automated unit tests under `commonTest`.
* **JDK requirement:** Always compile with native ARM64 JDK 17 to prevent Rosetta CPU errors.
