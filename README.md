# UNIVAULT 

[![Star this repo](https://img.shields.io/github/stars/ComradeMohan/192210400PDD?style=social)](https://github.com/ComradeMohan/192210400PDD/stargazers)
[![PHP](https://img.shields.io/badge/PHP-777BB4?logo=php&logoColor=white&style=flat-square)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white&style=flat-square)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white&style=flat-square)](#)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=white&style=flat-square)](#)

> **Tip:** If you like this project, please consider clicking the **"Star"** button near the top right of this page or use the badge above!

---

A full-stack academic management system:
- **Backend:** PHP with MySQL
- **Frontend:** Kotlin (Android)
- **Notifications & Auth:** Firebase

## Features

- **Admin, Faculty, and Student Authentication:** Secure login and registration for different roles.
- **Course Management:** Add, delete, and manage courses and departments.
- **Event and Notice Management:** Schedule, update, and delete events/notices.
- **Grades and Feedback:** Submit and retrieve student grades and feedback.
- **Notifications:** Push notifications via FCM, email notifications, and in-app notices.
- **Multi-role Dashboards:** Dedicated endpoints and panels for admin and faculty management.
- **Password Management:** Change and reset passwords.
- **Third-party Auth:** Google login integration via Firebase.

## Tech Stack

| Backend | Database | Frontend | Notifications/Auth |
|---------|----------|----------|-------------------|
| ![PHP](https://img.shields.io/badge/-PHP-777BB4?logo=php&logoColor=white&style=flat-square) | ![MySQL](https://img.shields.io/badge/-MySQL-4479A1?logo=mysql&logoColor=white&style=flat-square) | ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white&style=flat-square) | ![Firebase](https://img.shields.io/badge/-Firebase-FFCA28?logo=firebase&logoColor=white&style=flat-square) |

## Project Structure

- [`backend/`](https://github.com/ComradeMohan/192210400PDD/tree/main/backend): PHP backend scripts and API endpoints.
- `frontend/`: Kotlin Android app (use this directory or a linked repo).
- MySQL database for persistent storage.
- Firebase for push notifications and authentication.

## Setup Instructions

### Backend (PHP + MySQL)
1. Clone the repository:
   ```bash
   git clone https://github.com/ComradeMohan/192210400PDD.git
   ```
2. Set up your PHP server and MySQL.
3. Import the database schema (see your backend folder for `.sql` files or ask the maintainer).
4. Configure database connection in `backend/db.php`.
5. Install any dependencies (see `vendor/`).

### Frontend (Kotlin + Firebase)
1. Open `frontend/` (or your Android project) in Android Studio.
2. Link your Firebase project and download `google-services.json` to your app module.
3. Set up dependencies for Firebase Auth, Firestore, and FCM.

## Usage

- Use backend endpoints for management operations.
- Android app interacts with backend via REST APIs and Firebase services.
- Admins manage users, courses, and events. Faculty manage courses and student data. Students interact with notices, events, and grades.

## Contribution

1. Fork this repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes.
4. Push to your branch and open a Pull Request.
