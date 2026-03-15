# Online Examination System (Java Swing + MySQL)

A desktop-based **Online Examination System** built using **Java Swing and MySQL**.  
This application allows **students to register, login, take exams, and view results**, while **admins can manage students and view all exam results**.

The system supports **multiple-choice and descriptive questions**, includes a **timer with auto-submit**, and stores results in a **MySQL database**.


# Features

## Admin Features
- Admin login
- Add new students
- View all students' exam results
- Manage student accounts

## Student Features
- Student registration
- Student login
- Choose exam from dashboard
- Take online exam
- Automatic result calculation
- View exam score after submission

## Exam System
- Multiple Choice Questions (MCQ)
- Descriptive Questions
- Timer-based exam
- Auto-submit when time expires
- Different exams for different subjects

## Result Management
- Results stored in **MySQL database**
- Admin can view all results in a table
- Results include:
  - Student ID
  - Student name
  - Exam name
  - Score
  - Total marks
  - Date and time


# Technologies Used

## Programming Language
- Java

## GUI Framework
- Java Swing

## Database
- MySQL (XAMPP)

## Database Connectivity
- JDBC (Java Database Connectivity)


# Object-Oriented Programming Concepts Used

This project demonstrates several **OOP principles**:

### Encapsulation
Student class with private fields and getters/setters.

### Inheritance
- `MultipleChoiceQuestion`
- `DescriptiveQuestion`

Both extend the abstract `Question` class.

### Polymorphism
Different implementations of the `grade()` method for MCQ and descriptive questions.

### Abstraction
Abstract `Question` class defines the structure for all question types.


# Project Structure

```
OnlineExamSystem/
│
├── OnlineExamApp.java
│
├── Database
│   ├── students table
│   └── results table
│
└── MySQL (XAMPP)
```


# Database Setup

Create a database named:

```
online_exam
```

### Students Table

```sql
CREATE TABLE students (
    student_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100),
    password VARCHAR(100)
);
```

### Results Table

```sql
CREATE TABLE results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20),
    student_name VARCHAR(100),
    exam_name VARCHAR(100),
    score INT,
    total INT,
    taken_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```


# How to Run the Project

## 1. Install Requirements
- Java JDK
- XAMPP (MySQL)
- MySQL Connector/J (JDBC Driver)

## 2. Start MySQL Server
Start **Apache and MySQL** from XAMPP.

## 3. Create Database
Create the database and tables shown above.

## 4. Add MySQL Connector
Add **mysql-connector-java.jar** to your project classpath.

## 5. Compile and Run

```bash
javac OnlineExamApp.java
java OnlineExamApp
```


# Default Admin Login

```
Username: admin
Password: admin123
```


# Future Improvements

- Add **question management for admin**
- Support **more exam categories**
- Add **student result dashboard**
- Implement **password encryption**
- Add **web-based version using Spring Boot**
- Improve UI design


# Author

A. V. Samanvitha

Developed as a **Java Swing + MySQL project** demonstrating:
- GUI programming
- JDBC database connectivity
- Object-Oriented Programming
- Online examination workflow

---
