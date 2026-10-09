# MotoVault 🏍️⚡

> **Your vehicle's digital passport, maintenance vault, and lifecycle health tracker.**  
> Built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** for **Android & iOS**.

---

## 🌟 Key Features

### 1. Dynamic Powertrain Intelligence
- **KTM Duke 125 Indian Version Preset:** Pre-loaded factory maintenance schedule (Motul 7100 10W-50 oil, 500 km chain lube, spark plug, ByBre brake pads, coolant).
- **Emission & Edition Filtering:** Cascading selection tree where choosing **Indian Edition** filters strictly to **BS4** (underbelly exhaust, 10.2L tank) and **BS6** (side-slung exhaust, 13.5L tank).
- **Extensible to EV Bikes & Cars:** Polymorphic vehicle data model supporting electric charging metrics (kWh, battery SOH %, belt drive) and passenger cars.

### 2. High-Contrast TFT Cluster Design System
- **Vehicle-Adaptive Dynamic Accents:** The app's primary accent dynamically transforms to match your ride:
  - **KTM:** Apex Orange (`#FF6600`)
  - **Kawasaki:** Lime Green (`#00E676`)
  - **Yamaha:** Racing Blue (`#0066FF`)
  - **EVs:** Cyber Teal (`#00F2FE`)
- **Circular Radial Arc Gauges:** Tachometer/speedometer inspired wear meters with smooth animated sweeps and real-time color shifts ($\text{Green} \rightarrow \text{Yellow} \rightarrow \text{Red}$).
- **Persistent Floating Quick Action Dock:** 1-tap thumb buttons for `Lube Chain` (500 km reset), `Fuel Up`, and `Police Check`.

### 3. Bangladeshi Road Legal Suite & Document Vault
- **Skeuomorphic 3D Flip Smart Cards:** Exact replica of the official Bangladesh Government (BRTA) vehicle registration smart card and driving license with microchip, seal, and realistic 180° 3D flip.
- **🚨 1-Tap 'Traffic Police Check Mode':** Instantly triggers maximum display brightness and presents essential road papers with zero privacy leak of personal costs or service notes.
- **Expiry Countdowns:** Real-time day counters and proactive renewal alerts for Tax Token and Driving License.

---

## 🏗️ Project Architecture

```
motovault/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/
│   │   │   └── kotlin/com/motovault/app/
│   │   │       ├── App.kt                          # Root navigation & theme orchestration
│   │   │       ├── domain/
│   │   │       │   ├── model/                      # Vehicle, ComponentRule, GovernmentDocument
│   │   │       │   └── preset/KtmDuke125Preset.kt  # KTM factory service intervals
│   │   │       ├── theme/                          # Colors, Typography, Dynamic Theme
│   │   │       └── ui/
│   │   │           ├── onboarding/                 # Cascading Brand -> KTM -> Edition -> Emission
│   │   │           ├── garage/                     # Dashboard & Circular Wear Gauges
│   │   │           └── vault/                      # 3D Flip Smart Card & Police Check Mode
│   │   ├── androidMain/                            # Android native entry & Activity
│   │   └── iosMain/                                # iOS native framework bindings
└── gradle/libs.versions.toml                       # Dependency catalog (KMP 2.0, Compose 1.7)
```

---

## 🚀 Running the Project

### Prerequisites
- JDK 17 or higher (Apple Silicon ARM64 recommended)
- Android SDK (API 35 compileSdk, API 24 minSdk)
- Xcode (for iOS simulator or device deployment)

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### iOS
Open `iosApp` in Xcode or run via Fleet / Android Studio with the KMP plugin.
