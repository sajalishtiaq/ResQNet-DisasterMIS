DISASTER MIS - SMART DISASTER RESPONSE MANAGEMENT INFORMATION SYSTEM
====================================================================

A Full-Stack JavaFX + Microsoft SQL Server based Enterprise Disaster 
Management Information System (MIS)

Project Type: Database Systems Course Final Project
====================================================================

PROJECT OVERVIEW
----------------
Disaster MIS (ResQNet) is a comprehensive role-based system designed to 
coordinate disaster response operations efficiently. It manages emergency 
reports, rescue teams, hospital resources, inventory, and financial 
transactions during disaster situations.

TECHNOLOGIES USED
-----------------
• Frontend     : JavaFX 21
• Backend      : Java 17
• Database     : Microsoft SQL Server 2019/2022
• Build Tool   : Apache Maven
• Architecture : MVC + DAO Pattern

PREREQUISITES
-------------
1. Java JDK 17 or higher
2. Apache Maven 3.8+
3. SQL Server Express (Instance: LALEYKA\SQLEXPRESS)
4. SQL Server Management Studio (SSMS)

INSTALLATION & SETUP
--------------------

Step 1: Database Setup
----------------------
1. Open SQL Server Management Studio (SSMS)
2. Connect to server: LALEYKA\SQLEXPRESS
3. Open the file "Deliverable4.sql"
4. Execute the entire script (Press F5)

Step 2: Create Database User
----------------------------
Run the following SQL commands:

CREATE LOGIN app_user WITH PASSWORD = 'sajal1234567';
USE DisasterMIS;
CREATE USER app_user FOR LOGIN app_user;
ALTER ROLE db_datareader ADD MEMBER app_user;
ALTER ROLE db_datawriter ADD MEMBER app_user;
GRANT EXECUTE TO app_user;

Step 3: Run the Application
---------------------------
Open terminal in project folder and run:

mvn clean javafx:run

LOGIN CREDENTIALS
-----------------
Default Password for all accounts: Password@123

Role                  | Email                        | Access
----------------------|------------------------------|------------------------
Administrator         | admin@disastermis.pk         | Full System Access
Emergency Operator    | sara@disastermis.pk          | Report Management
Emergency Operator    | maria@disastermis.pk         | Report Management
Field Officer         | ali@disastermis.pk           | Field Operations
Field Officer         | omar@disastermis.pk          | Field Operations
Warehouse Manager     | hina@disastermis.pk          | Inventory & Resources
Finance Officer       | zain@disastermis.pk          | Financial Management

KEY FEATURES
------------
• Role-based dashboards (7 different interfaces)
• Real-time Emergency Report Management
• Rescue Team Assignment & Tracking
• Hospital & Patient Management
• Inventory Management with Low Stock Alerts
• Resource Allocation System
• Financial Transaction & Budget Management
• Centralized Approval Workflow
• Complete Audit Trail (Auto-logged by Triggers)

DATABASE FEATURES IMPLEMENTED
-----------------------------
• 22 Tables with proper relationships
• 8 Views for secure data access
• 5 Stored Procedures with ACID transactions
• 7 Triggers for automatic logging and updates
• Comprehensive Indexing for performance
• Role-Based Access Control (RBAC)

PROJECT STRUCTURE
-----------------
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

NOTES
-----
• **Passwords are hashed using SHA-256 (matches SQL Server HASHBYTES)
• All critical operations use database triggers and stored procedures
• System is designed for real-world disaster response scenarios**
====================================================================
Thank You!
