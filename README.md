# 🐾 Pet Care Reminder App

I created this app because I needed a simple, shared way for my family to manage our cat, Dorito's care. It makes it super easy for everyone at home to see who fed him, when, and what, so we can prevent both overfeeding and those "oops, did anyone feed Dorito?" moments. On top of that, this app also includes setting reminders for all the crucial pet stuff – like meal times, when to buy more food or litter, and staying on top of vet appointments. Consider it your family's central hub for simple, coordinated pet care!


---

## Features

- **Multi-User Support**  
  Multiple family members can log in and share pet care duties.

- **Feeding Tracker**  
  Record when and who fed the pet.

- **Vet Appointment Scheduler**  
  Add reminders for vaccinations and vet visits.

- **Food Stock Alerts**  
  Never forget to buy pet food again!

- **Photo Capture**  
  inclusive of a gallery where u can capture and store your pet's photos



---

## App Screenshots

![image](https://github.com/user-attachments/assets/1bfaee8e-ad73-4496-b363-976e14800ab3)
![image](https://github.com/user-attachments/assets/6d631b64-d78c-46bd-871f-4544941f5e36)
![image](https://github.com/user-attachments/assets/ac27d65b-e42d-43fd-80ba-8a07a4934327)
![image](https://github.com/user-attachments/assets/7c29bffc-6c03-4d7b-966c-6be642c61f45)
![image](https://github.com/user-attachments/assets/287dedce-8be8-4a92-b4bc-39367187ef60)
![image](https://github.com/user-attachments/assets/34f199a4-6d3f-4b62-a705-0be85196319d)

---

## Architecture

- **Language:** Kotlin  
- **UI:** XML Layouts  
- **App Structure:** Fragment-based UI  
- **Database:** Firebase Firestore  
- **Authentication:** Firebase Auth (Email/Password)

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


## 🙌 Acknowledgements

- [Firebase Documentation](https://firebase.google.com/docs/firestore)  
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)  
- Dorito, for the inspiration 🐱
