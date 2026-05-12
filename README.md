# 🚨 DISASTER MIS — ResQNet
### Smart Disaster Response Management Information System

> A Full-Stack **JavaFX + Microsoft SQL Server** based Enterprise Disaster Management Information System (MIS)
>
> **Project Type:** Database Systems Course Final Project

---

## 📖 Project Overview

**Disaster MIS (ResQNet)** is a comprehensive role-based system designed to coordinate disaster response operations efficiently. It manages emergency reports, rescue teams, hospital resources, inventory, and financial transactions during disaster situations.

---

## 🛠️ Technologies Used

| Layer | Technology |
|---|---|
| Frontend | JavaFX 21 |
| Backend | Java 17 |
| Database | Microsoft SQL Server 2019/2022 |
| Build Tool | Apache Maven |
| Architecture | MVC + DAO Pattern |

---

## ✅ Prerequisites

1. Java JDK 17 or higher
2. Apache Maven 3.8+
3. SQL Server Express (Instance: `LALEYKA\SQLEXPRESS`)
4. SQL Server Management Studio (SSMS)

---

## ⚙️ Installation & Setup

### Step 1: Database Setup

1. Open **SQL Server Management Studio (SSMS)**
2. Connect to server: `LALEYKA\SQLEXPRESS`
3. Open the file `Deliverable4.sql`
4. Execute the entire script *(Press F5)*

### Step 2: Create Database User

Run the following SQL commands:

```sql
CREATE LOGIN app_user WITH PASSWORD = 'sajal1234567';
USE DisasterMIS;
CREATE USER app_user FOR LOGIN app_user;
ALTER ROLE db_datareader ADD MEMBER app_user;
ALTER ROLE db_datawriter ADD MEMBER app_user;
GRANT EXECUTE TO app_user;
```

### Step 3: Run the Application

Open a terminal in the project folder and run:

```bash
mvn clean javafx:run
```

---

## 🔐 Login Credentials

> **Default Password for all accounts:** `Password@123`

| Role | Email | Access |
|---|---|---|
| Administrator | admin@disastermis.pk | Full System Access |
| Emergency Operator | sara@disastermis.pk | Report Management |
| Emergency Operator | maria@disastermis.pk | Report Management |
| Field Officer | ali@disastermis.pk | Field Operations |
| Field Officer | omar@disastermis.pk | Field Operations |
| Warehouse Manager | hina@disastermis.pk | Inventory & Resources |
| Finance Officer | zain@disastermis.pk | Financial Management |

---

## ✨ Key Features

- 🧑‍💼 Role-based dashboards *(7 different interfaces)*
- 📋 Real-time Emergency Report Management
- 🚑 Rescue Team Assignment & Tracking
- 🏥 Hospital & Patient Management
- 📦 Inventory Management with Low Stock Alerts
- 🔧 Resource Allocation System
- 💰 Financial Transaction & Budget Management
- ✅ Centralized Approval Workflow
- 📜 Complete Audit Trail *(Auto-logged by Triggers)*

---

## 🗄️ Database Features Implemented

| Feature | Count |
|---|---|
| Tables (with proper relationships) | 22 |
| Views (for secure data access) | 8 |
| Stored Procedures (with ACID transactions) | 5 |
| Triggers (automatic logging & updates) | 7 |

- ⚡ Comprehensive Indexing for performance
- 🔒 Role-Based Access Control (RBAC)

---

## 📁 Project Structure

```
DisasterMIS/
├── pom.xml
├── README.md
├── Deliverable4.sql
└── src/main/
    ├── java/com/disastermis/
    │   ├── MainApp.java
    │   ├── controller/
    │   ├── dao/
    │   ├── db/
    │   └── util/
    └── resources/
        ├── fxml/
        └── css/
```

---

## 📝 Notes

- Passwords are hashed using **SHA-256** (matches SQL Server `HASHBYTES`)
- All critical operations use database **triggers** and **stored procedures**
- System is designed for real-world disaster response scenarios

---

<div align="center">

**Thank You! 🙏**

</div>
