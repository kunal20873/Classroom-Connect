ClassroomConnect is an Android application made to manage classroom activities in a simple way.
This app helps teachers and students stay connected through classes, study materials, and discussion forums.
I built this project as a learning-based academic project using Kotlin and Firebase, focusing on real app-like features instead of just UI.

 What this app can do?

 Teacher side
Create classes
Share study materials
Delete uploaded materials
View and reply to student doubts
Manage discussion forum messages
Logout using navigation drawer

 Student side
Join classes using class code.
View materials uploaded by teacher.
Ask doubts in discussion forum.
Read teacher replies.
Logout using navigation drawer

 Discussion Forum
Common discussion space for each class.
Students can ask doubts.
Teacher can reply.
Messages appear in real time using Firebase Realtime Database.
Teacher has permission to delete inappropriate discussion messages.

UI & UX
Clean and simple UI
Uses Material Design
Supports Light mode & Dark mode
Navigation Drawer with user info and logout option

Proper empty states:
No class joined
No class created

 Authentication & Roles
Firebase Email & Password Authentication
Role-based access:
Teacher
Student
After login, user is redirected based on role
User stays logged in until logout

 Tech Used
Language: Kotlin
Database: Firebase Realtime Database
Authentication: Firebase Authentication
UI: XML + Material Design
Version Control: Git & GitHub
