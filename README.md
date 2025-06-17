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

![Screenshot_20250617_050907](https://github.com/user-attachments/assets/1c8970b9-63d7-43e8-948f-a4209be983d7)
![Screenshot_20250617_050229 (2)](https://github.com/user-attachments/assets/d19a68bd-9953-424e-a920-2a977a0e065b)
![Screenshot_20250617_051431](https://github.com/user-attachments/assets/f91a725e-26fa-44cf-839d-8fade6123596)
![Screenshot_20250617_051530](https://github.com/user-attachments/assets/739a9f70-c7f2-4a04-aa63-b6e9788e442a)
![Screenshot_20250617_051705](https://github.com/user-attachments/assets/9e7054a1-1d8a-490d-a22b-3c47107f6b3c)
![Screenshot_20250617_051943](https://github.com/user-attachments/assets/84f7e43e-119e-4d86-8c6c-01d7ec579402)
![Screenshot_20250617_052100](https://github.com/user-attachments/assets/fac04d0b-e8ca-4137-a036-ad4f3d34738e)

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
