# SyncU

**Seamlessly sync your Health Connect data to Intervals.icu**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![Health Connect](https://img.shields.io/badge/Health%20Connect-Supported-teal.svg)](https://health.google/health-connect/)

SyncU is a lightweight Android app that automatically syncs your health and fitness data from Health Connect to Intervals.icu. No manual exports, no complicated setup - just seamless data syncing.

---

## ✨ Features

- **📊 Side-by-Side Comparison** - View your Health Connect data alongside Intervals.icu data
- **🔄 Automatic Sync** - Set it to sync twice daily and forget about it
- **⚡ Manual Control** - Refresh data or sync specific days on demand
- **🔒 Privacy First** - Data syncs directly from your device to Intervals.icu
- **📱 Simple & Clean** - Minimal interface focused on data accuracy
- **🆓 Free & Open Source** - No ads, no tracking, no premium features

---

## 📸 Screenshots

_Coming soon_

---

## 🚀 Getting Started

### Requirements

- Android device with Health Connect support
- Intervals.icu account with API access
- Health Connect compatible apps (Google Fit, Samsung Health, Garmin, Fitbit, etc.)

### Installation

1. Download from [Google Play Store](#) _(coming soon)_
2. Or build from source (see below)

### Setup

1. **Grant Permissions** - Allow SyncU to read data from Health Connect
2. **Add Credentials** - Enter your Intervals.icu API ID and key
3. **Enable Auto-Sync** - Optionally enable twice-daily automatic syncing
4. **Done!** - View side-by-side data and sync as needed

---

## 🔄 How It Works

SyncU acts as a bridge between Health Connect and Intervals.icu:

```
Health Connect → SyncU (on your device) → Intervals.icu
```

### Data Synced

- **Activities** - Running, cycling, walking, and other workouts
- **Heart Metrics** - Resting heart rate, average HR, HRV
- **Steps & Distance** - Daily step counts and distance covered
- **Calories** - Active and total energy burned
- **Sleep** - Total sleep time and sleep stages (awake, light, deep, REM)
- **Body Metrics** - Weight and body composition

### Sync Options

**Automatic Sync:**
- Enable in settings
- Syncs twice per day automatically
- Keeps your Intervals.icu data up to date

**Manual Sync:**
- Pull down to refresh Health Connect data
- Tap sync button to push specific days to Intervals.icu
- Full control when you need it

---

## 🔒 Privacy & Security

**Your data stays yours.**

- ✅ No data collection - we don't store or access your health data
- ✅ Direct sync - data goes straight from your device to Intervals.icu
- ✅ Local credentials - your API keys are encrypted on your device only
- ✅ Open source - verify our claims by reading the code

[Read our Privacy Policy](https://yourusername.github.io/syncu/privacy.html)

---

## 🛠️ Building from Source

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 36
- Gradle 8.11

### Build Steps

```bash
# Clone the repository
git clone https://github.com/dromeas-developer/syncu.git
cd syncu

# Open in Android Studio
# Or build from command line:
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Configuration

No special configuration needed - just build and run!

---

## 📱 App Architecture

- **Language:** Kotlin
- **UI:** Android Jetpack Compose / XML layouts
- **Database:** Room (SQLite)
- **Health Connect:** Health Connect API 1.1.0-alpha10
- **Networking:** Retrofit for Intervals.icu API
- **Security:** Android Keystore for credential encryption

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

- 🐛 Report bugs via [GitHub Issues](https://github.com/yourusername/syncu/issues)
- 💡 Suggest features or improvements
- 🔧 Submit pull requests
- 📖 Improve documentation
- ⭐ Star the project if you find it useful!

### Development Guidelines

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 🐛 Known Issues

- None currently! Report issues on [GitHub](https://github.com/yourusername/syncu/issues)

---

## 📋 Roadmap

- [x] Basic Health Connect sync
- [x] Side-by-side data comparison
- [x] Auto-sync (twice daily)
- [x] Manual refresh and sync
- [ ] Google Play Store release
- [ ] oAuth intervals.icu authentication

---

## ❓ FAQ

### Does SyncU replace the Intervals.icu app?
No, SyncU is a companion tool that syncs Health Connect data TO Intervals.icu. You still use Intervals.icu for training analysis.

### Does SyncU work without internet?
You can view your local Health Connect data offline, but syncing to Intervals.icu requires an internet connection.

### What happens if I have different data in Health Connect vs Intervals.icu?
SyncU shows both side-by-side. When you sync, Health Connect data is pushed to Intervals.icu (it doesn't merge or modify).

### How often does auto-sync run?
Twice daily when enabled. You can also manually sync anytime.

### Is my data safe?
Yes. SyncU doesn't store your health data on any servers. Your Intervals.icu credentials are encrypted using Android Keystore and stored only on your device.

### Which Health Connect apps are compatible?
Any app that writes to Health Connect: Google Fit, Samsung Health, Fitbit, Garmin Connect, Strava, and many more.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Intervals.icu](https://intervals.icu/) - For their excellent fitness analytics platform
- [Health Connect](https://health.google/health-connect/) - For Android's unified health data platform
- The open source community

---

## 📧 Support

- **Issues:** [GitHub Issues](https://github.com/yourusername/syncu/issues)
- **Email:** support@dromeas.dev
- **Privacy Policy:** [privacy.html](https://yourusername.github.io/syncu/privacy.html)
- **Terms & Conditions:** [terms.html](https://yourusername.github.io/syncu/terms.html)

---

## 🌟 Star History

If you find SyncU useful, please consider starring the repository! ⭐

---
**Made with ❤️ for athletes and fitness enthusiasts**

SyncU - Because your fitness data should flow freely. 🏃‍♂️💓📊
