<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f172a,100:14b8a6&height=220&section=header&text=UniVault&fontSize=64&fontColor=ffffff&animation=fadeIn&fontAlignY=35&desc=Full-Stack%20Academic%20Management%20System&descAlignY=55&descSize=18&fontAlign=50" width="100%"/>

<a href="https://readme-typing-svg.demolab.com">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=20&duration=3000&pause=1200&color=14B8A6&center=true&vCenter=true&width=600&lines=PHP+%2B+MySQL+%2B+Kotlin+%2B+Firebase;Multi-Role+Academic+Dashboards;Live+on+the+Google+Play+Store" alt="Typing SVG" />
</a>

<br/>

[![Stars](https://img.shields.io/github/stars/ComradeMohan/192210400PDD?style=for-the-badge&logo=github&color=14b8a6&labelColor=0f172a)](https://github.com/ComradeMohan/192210400PDD/stargazers)
[![Forks](https://img.shields.io/github/forks/ComradeMohan/192210400PDD?style=for-the-badge&logo=github&color=14b8a6&labelColor=0f172a)](https://github.com/ComradeMohan/192210400PDD/network/members)
[![Last Commit](https://img.shields.io/github/last-commit/ComradeMohan/192210400PDD?style=for-the-badge&logo=github&color=14b8a6&labelColor=0f172a)](https://github.com/ComradeMohan/192210400PDD/commits/main)
[![Repo Size](https://img.shields.io/github/repo-size/ComradeMohan/192210400PDD?style=for-the-badge&logo=github&color=14b8a6&labelColor=0f172a)](https://github.com/ComradeMohan/192210400PDD)

[![PHP](https://img.shields.io/badge/PHP-777BB4?style=for-the-badge&logo=php&logoColor=white&labelColor=0f172a)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white&labelColor=0f172a)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0f172a)](#)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white&labelColor=0f172a)](#)

[![Website](https://img.shields.io/badge/Website-univault.live-14b8a6?style=for-the-badge&logo=googlechrome&logoColor=white&labelColor=0f172a)](https://univault.live)

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.simats.univault">
    <img
      src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
      height="70"
      alt="Get it on Google Play">
  </a>
</p>

</div>

<p align="center">
  <b>Star this repo</b> if UniVault is useful to you — it genuinely helps 🌟
</p>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Screenshots](#-screenshots)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Usage](#-usage)
- [Contributing](#-contributing)
- [Author](#-author)

---

## 🧭 Overview

**UniVault** is a full-stack academic management system built for institutions to manage courses, events, grades, and multi-role communication — all in one place.

- 🖥️ **Backend:** PHP + MySQL REST API
- 📱 **Frontend:** Native Android app in Kotlin
- 🔔 **Auth & Notifications:** Firebase (Auth, FCM, Google Sign-In)

---

## ✨ Features

<table>
<tr>
<td width="50%">

**🔐 Authentication**
- Role-based login for Admin, Faculty, and Students
- Google Sign-In via Firebase
- Secure password change & reset flows

**📚 Course Management**
- Add, update, and delete courses & departments
- Faculty-to-course assignment

</td>
<td width="50%">

**📅 Events & Notices**
- Schedule, update, and remove events/notices
- Real-time push notifications via FCM

**📊 Grades & Feedback**
- Submit and retrieve student grades
- Structured feedback collection

</td>
</tr>
<tr>
<td width="50%">

**🧑‍💼 Multi-Role Dashboards**
- Dedicated panels for Admin and Faculty
- Distinct endpoints per role

</td>
<td width="50%">

**📩 Notifications**
- Push (FCM), email, and in-app notices
- Keeps students and faculty in sync

</td>
</tr>
</table>

---

## 🛠 Tech Stack

<div align="center">
<img src="tech-stack.svg" width="100%" alt="Tech stack animated diagram" />
</div>

<!-- Place the tech-stack.svg file in an `assets/` folder at the repo root so the path above resolves. -->
<!--
| Backend | Database | Frontend | Notifications/Auth |
|---------|----------|----------|-------------------|
| ![PHP](https://img.shields.io/badge/-PHP-777BB4?logo=php&logoColor=white&style=flat-square) | ![MySQL](https://img.shields.io/badge/-MySQL-4479A1?logo=mysql&logoColor=white&style=flat-square) | ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white&style=flat-square) | ![Firebase](https://img.shields.io/badge/-Firebase-FFCA28?logo=firebase&logoColor=white&style=flat-square) |
-->
---

## 🏗 Architecture

```mermaid
flowchart LR
    subgraph Client["📱 Android App (Kotlin)"]
        UI[UI Layer]
        VM[Repository / ViewModel]
    end

    subgraph Cloud["🔥 Firebase"]
        Auth[Firebase Auth + Google Sign-In]
        FCM[Cloud Messaging]
    end

    subgraph Server["🖥️ PHP Backend"]
        API[REST API Endpoints]
    end

    subgraph DB["🗄️ MySQL"]
        Tables[(Users · Courses · Events · Grades)]
    end

    UI --> VM
    VM -->|REST calls| API
    VM -->|Sign-in| Auth
    Auth -.-> FCM
    API --> Tables
    FCM -->|Push Notifications| UI
```

---

## 📸 Screenshots

<div align="center">
<!-- Replace with real screenshots: drag images into an issue/PR to get a hosted URL, or add them to a /screenshots folder and reference here -->
<img src="./univault_banner.png" alt = "univault banner image">
</div>

---

## 📁 Project Structure

```
192210400PDD/
├── assets/            # Static images used in this README (e.g. tech-stack.svg)
├── backend/           # PHP backend scripts & API endpoints
│   └── *.sql            # Database schema files
├── frontend/          # Kotlin Android app
└── README.md
```

- [`backend/`](https://github.com/ComradeMohan/192210400PDD/tree/main/backend) — PHP backend scripts and API endpoints
- `frontend/` — Kotlin Android app (or linked repo)
- MySQL — persistent storage
- Firebase — push notifications & authentication

---

## 🚀 Getting Started

### Backend (PHP + MySQL)

```bash
git clone https://github.com/ComradeMohan/192210400PDD.git
```

1. Set up your PHP server and MySQL instance.
2. Import the database schema (`.sql` files under `backend/`).
3. Configure the database connection in `backend/db.php`.
4. Install any dependencies (see `vendor/`).

### Frontend (Kotlin + Firebase)

1. Open `frontend/` (or your Android project) in Android Studio.
2. Link your Firebase project and drop `google-services.json` into the app module.
3. Enable Firebase Auth, Firestore, and FCM for the project.

---

## 💡 Usage

- Backend endpoints handle all management operations.
- The Android app talks to the backend via REST APIs and to Firebase directly for auth/notifications.
- **Admins** manage users, courses, and events. **Faculty** manage courses and student data. **Students** interact with notices, events, and grades.

---

## 🤝 Contributing

1. Fork this repository
2. Create a feature branch — `git checkout -b feature-name`
3. Commit your changes
4. Push to your branch and open a Pull Request

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-14b8a6?style=for-the-badge&labelColor=0f172a)](https://github.com/ComradeMohan/192210400PDD/pulls)

---


<!--
## ⭐ Star History
<a href="https://star-history.com/#ComradeMohan/192210400PDD&Date">
  <img src="https://api.star-history.com/svg?repos=ComradeMohan/192210400PDD&type=Date" width="600"/>
</a> -->

---

## 👤 Author

<div align="center">

**Comrade Mohan** — Founder & Developer

[![GitHub](https://img.shields.io/badge/GitHub-ComradeMohan-14b8a6?style=for-the-badge&logo=github&logoColor=white&labelColor=0f172a)](https://github.com/ComradeMohan)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-mmohanreddy-14b8a6?style=for-the-badge&logo=linkedin&logoColor=white&labelColor=0f172a)](https://linkedin.com/in/mmohanreddy)
[![Portfolio](https://img.shields.io/badge/Portfolio-mohanreddy.me-14b8a6?style=for-the-badge&logo=vercel&logoColor=white&labelColor=0f172a)](https://mohanreddy.me)

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:14b8a6,100:0f172a&height=100&section=footer" width="100%"/>

</div>
