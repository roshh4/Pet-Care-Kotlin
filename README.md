# 🐾 Pet Care Reminder App

A feature-rich Android application built using Kotlin and XML layouts, designed to help families collaboratively manage every aspect of their pet’s care — from feeding routines to medical appointments.

---

## 📱 Features

- 👥 **Multi-User Support**  
  Multiple family members can log in and share pet care duties.

- 🍽️ **Feeding Tracker**  
  Record when and who fed the pet.

- 🩺 **Vet Appointment Scheduler**  
  Add reminders for vaccinations and vet visits.

- 💩 **Litter Box & Hygiene Tracker**  
  Regular reminders for cleaning tasks.

- 🛒 **Food Stock Alerts**  
  Never forget to buy pet food again!

- 📸 **Photo Capture**  
  Snap vet prescriptions, bills, or pet pics!

- ⏰ **Smart Notifications**  
  AlarmManager and NotificationManager based alerts.

---

## 🧩 Architecture

- **Language:** Kotlin  
- **UI:** XML Layouts  
- **App Structure:** Fragment-based UI  
- **Database:** Firebase Firestore  
- **Authentication:** Firebase Auth (Email/Password)  
- **Notifications:** Local Alarms + System Notifications

---

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Ensure you have the following prerequisites:
   - Android Studio Arctic Fox or newer
   - JDK 11
   - Android SDK 35
   - Google Play Services SDK

4. Add your `google-services.json` file to the `app` directory
   - This file can be obtained from the Firebase Console
   - Make sure to register your application in Firebase Console first

5. Sync the project with Gradle files
6. Build and run the application

## Building the Project

```bash
# Using Gradle
./gradlew build

# Using Android Studio
# Simply click the "Run" button or press Shift + F10
```

## Testing

The project includes both unit tests and instrumentation tests:
- Unit tests using JUnit
- UI tests using Espresso
- Android instrumentation tests

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details

## Contact

For any queries or support, please open an issue in the repository. 


## 🙌 Acknowledgements

- [Firebase Documentation](https://firebase.google.com/docs/firestore)  
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)  
- Dorito, for the inspiration 🐱
