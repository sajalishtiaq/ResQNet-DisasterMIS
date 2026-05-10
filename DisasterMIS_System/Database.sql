CREATE DATABASE DisasterMIS;

USE DisasterMIS;

-- DDL – TABLE CREATION

-- 1.1  ROLE
CREATE TABLE ROLE (
    RoleID      INT          NOT NULL IDENTITY(1,1),
    RoleName    VARCHAR(50)  NOT NULL,
    CONSTRAINT PK_ROLE      PRIMARY KEY (RoleID),
    CONSTRAINT UQ_ROLE_Name UNIQUE      (RoleName)
);

-- 1.2  USER
CREATE TABLE [USER] (
    UserID       INT          NOT NULL IDENTITY(1,1),
    FullName     VARCHAR(100) NOT NULL,
    Email        VARCHAR(150) NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    PhoneNumber  VARCHAR(20)  NULL,
    IsActive     BIT          NOT NULL DEFAULT 1,
    RoleID       INT          NOT NULL,
    CONSTRAINT PK_USER       PRIMARY KEY (UserID),
    CONSTRAINT UQ_USER_Email UNIQUE      (Email),
    CONSTRAINT FK_USER_ROLE  FOREIGN KEY (RoleID) REFERENCES ROLE(RoleID)
);

-- 1.3  AUDITLOG
CREATE TABLE AUDITLOG (
    LogID         INT            NOT NULL IDENTITY(1,1),
    ActionType    VARCHAR(50)    NOT NULL,
    TableAffected VARCHAR(100)   NOT NULL,
    OldValue      NVARCHAR(MAX)  NULL,
    NewValue      NVARCHAR(MAX)  NULL,
    LogTimestamp  DATETIME       NOT NULL DEFAULT GETDATE(),
    UserID        INT            NULL,
    CONSTRAINT PK_AUDITLOG      PRIMARY KEY (LogID),
    CONSTRAINT FK_AUDITLOG_USER FOREIGN KEY (UserID) REFERENCES [USER](UserID)
);

-- 1.4  CITIZEN
CREATE TABLE CITIZEN (
    CitizenID     INT          NOT NULL IDENTITY(1,1),
    FirstName     VARCHAR(50)  NOT NULL,
    LastName      VARCHAR(50)  NOT NULL,
    ContactNumber VARCHAR(20)  NULL,
    Street        VARCHAR(150) NULL,
    City          VARCHAR(100) NULL,
    Region        VARCHAR(100) NULL,
    CONSTRAINT PK_CITIZEN PRIMARY KEY (CitizenID)
);

-- 1.5  DISASTEREVENT
CREATE TABLE DISASTEREVENT (
    EventID        INT            NOT NULL IDENTITY(1,1),
    EventName      VARCHAR(150)   NOT NULL,
    DisasterType   VARCHAR(50)    NOT NULL,
    StartDate      DATE           NOT NULL,
    EndDate        DATE           NULL,
    AffectedRegion VARCHAR(200)   NOT NULL,
    Status         VARCHAR(30)    NOT NULL DEFAULT 'Active',
    TotalBudget    DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT PK_DISASTEREVENT PRIMARY KEY (EventID)
);

-- 1.6  EMERGENCYREPORT
CREATE TABLE EMERGENCYREPORT (
    ReportID      INT            NOT NULL IDENTITY(1,1),
    Latitude      DECIMAL(9,6)   NOT NULL,
    Longitude     DECIMAL(9,6)   NOT NULL,
    DisasterType  VARCHAR(50)    NOT NULL,
    SeverityLevel VARCHAR(20)    NOT NULL,
    TimeOfReport  DATETIME       NOT NULL DEFAULT GETDATE(),
    Status        VARCHAR(30)    NOT NULL DEFAULT 'Pending',
    Description   NVARCHAR(MAX)  NULL,
    CitizenID     INT            NULL,
    EventID       INT            NULL,
    CONSTRAINT PK_EMERGENCYREPORT PRIMARY KEY (ReportID),
    CONSTRAINT FK_REPORT_CITIZEN  FOREIGN KEY (CitizenID) REFERENCES CITIZEN(CitizenID),
    CONSTRAINT FK_REPORT_EVENT    FOREIGN KEY (EventID)   REFERENCES DISASTEREVENT(EventID)
);

-- 1.7  HOSPITAL
CREATE TABLE HOSPITAL (
    HospitalID        INT           NOT NULL IDENTITY(1,1),
    Name              VARCHAR(150)  NOT NULL,
    Address           VARCHAR(250)  NULL,
    City              VARCHAR(100)  NULL,
    Latitude          DECIMAL(9,6)  NULL,
    Longitude         DECIMAL(9,6)  NULL,
    ContactNumber     VARCHAR(20)   NULL,
    TotalBeds         INT           NOT NULL DEFAULT 0,
    EmergencyCapacity INT           NOT NULL DEFAULT 0,
    CONSTRAINT PK_HOSPITAL PRIMARY KEY (HospitalID)
);

-- 1.8  PATIENT
CREATE TABLE PATIENT (
    PatientID  INT          NOT NULL IDENTITY(1,1),
    FirstName  VARCHAR(50)  NOT NULL,
    LastName   VARCHAR(50)  NOT NULL,
    Age        INT          NULL,
    Gender     VARCHAR(10)  NULL,
    Condition  VARCHAR(100) NULL,
    CONSTRAINT PK_PATIENT PRIMARY KEY (PatientID)
);

-- 1.9  PATIENTADMISSION
CREATE TABLE PATIENTADMISSION (
    AdmissionID   INT      NOT NULL IDENTITY(1,1),
    AssignedAt    DATETIME NOT NULL DEFAULT GETDATE(),
    AdmissionTime DATETIME NOT NULL DEFAULT GETDATE(),
    DischargeTime DATETIME NULL,
    PatientID     INT      NOT NULL,
    HospitalID    INT      NOT NULL,
    ReportID      INT      NULL,
    AssignedBy    INT      NULL,
    CONSTRAINT PK_PATIENTADMISSION     PRIMARY KEY (AdmissionID),
    CONSTRAINT FK_ADMISSION_PATIENT    FOREIGN KEY (PatientID)  REFERENCES PATIENT(PatientID),
    CONSTRAINT FK_ADMISSION_HOSPITAL   FOREIGN KEY (HospitalID) REFERENCES HOSPITAL(HospitalID),
    CONSTRAINT FK_ADMISSION_REPORT     FOREIGN KEY (ReportID)   REFERENCES EMERGENCYREPORT(ReportID),
    CONSTRAINT FK_ADMISSION_ASSIGNEDBY FOREIGN KEY (AssignedBy) REFERENCES [USER](UserID)
);

-- 1.10  RESCUETEAM
CREATE TABLE RESCUETEAM (
    TeamID             INT          NOT NULL IDENTITY(1,1),
    TeamName           VARCHAR(100) NOT NULL,
    TeamType           VARCHAR(50)  NOT NULL,
    Latitude           DECIMAL(9,6) NULL,
    Longitude          DECIMAL(9,6) NULL,
    AvailabilityStatus VARCHAR(30)  NOT NULL DEFAULT 'Available',
    ContactNumber      VARCHAR(20)  NULL,
    Capacity           INT          NOT NULL DEFAULT 10,
    CONSTRAINT PK_RESCUETEAM PRIMARY KEY (TeamID)
);

-- 1.11  TEAMMEMBER
CREATE TABLE TEAMMEMBER (
    MemberID    INT          NOT NULL IDENTITY(1,1),
    FirstName   VARCHAR(50)  NOT NULL,
    LastName    VARCHAR(50)  NOT NULL,
    Designation VARCHAR(100) NULL,
    PhoneNumber VARCHAR(20)  NULL,
    TeamID      INT          NOT NULL,
    CONSTRAINT PK_TEAMMEMBER  PRIMARY KEY (MemberID),
    CONSTRAINT FK_MEMBER_TEAM FOREIGN KEY (TeamID) REFERENCES RESCUETEAM(TeamID)
);

-- 1.12  SPECIALIZATION
CREATE TABLE SPECIALIZATION (
    MemberID       INT          NOT NULL,
    Specialization VARCHAR(100) NOT NULL,
    CONSTRAINT PK_SPECIALIZATION PRIMARY KEY (MemberID, Specialization),
    CONSTRAINT FK_SPEC_MEMBER    FOREIGN KEY (MemberID) REFERENCES TEAMMEMBER(MemberID)
);

-- 1.13  TEAMASSIGNMENT
CREATE TABLE TEAMASSIGNMENT (
    AssignmentID INT      NOT NULL IDENTITY(1,1),
    AssignedAt   DATETIME NOT NULL DEFAULT GETDATE(),
    CompletedAt  DATETIME NULL,
    AssignedBy   INT      NULL,
    TeamID       INT      NOT NULL,
    ReportID     INT      NOT NULL,
    CONSTRAINT PK_TEAMASSIGNMENT        PRIMARY KEY (AssignmentID),
    CONSTRAINT FK_ASSIGNMENT_ASSIGNEDBY FOREIGN KEY (AssignedBy) REFERENCES [USER](UserID),
    CONSTRAINT FK_ASSIGNMENT_TEAM       FOREIGN KEY (TeamID)     REFERENCES RESCUETEAM(TeamID),
    CONSTRAINT FK_ASSIGNMENT_REPORT     FOREIGN KEY (ReportID)   REFERENCES EMERGENCYREPORT(ReportID)
);

-- 1.14  TEAMACTIVITYLOG
CREATE TABLE TEAMACTIVITYLOG (
    ActivityID     INT            NOT NULL IDENTITY(1,1),
    StartTime      DATETIME       NOT NULL DEFAULT GETDATE(),
    OutcomeSummary NVARCHAR(MAX)  NULL,
    Status         VARCHAR(30)    NOT NULL DEFAULT 'In Progress',
    EndTime        DATETIME       NULL,
    TeamID         INT            NOT NULL,
    CONSTRAINT PK_TEAMACTIVITYLOG  PRIMARY KEY (ActivityID),
    CONSTRAINT FK_ACTIVITYLOG_TEAM FOREIGN KEY (TeamID) REFERENCES RESCUETEAM(TeamID)
);

-- 1.15  WAREHOUSE
CREATE TABLE WAREHOUSE (
    WarehouseID INT          NOT NULL IDENTITY(1,1),
    Name        VARCHAR(150) NOT NULL,
    Address     VARCHAR(250) NULL,
    City        VARCHAR(100) NULL,
    Latitude    DECIMAL(9,6) NULL,
    Longitude   DECIMAL(9,6) NULL,
    Capacity    INT          NOT NULL DEFAULT 1000,
    ManagerID   INT          NULL,
    CONSTRAINT PK_WAREHOUSE         PRIMARY KEY (WarehouseID),
    CONSTRAINT FK_WAREHOUSE_MANAGER FOREIGN KEY (ManagerID) REFERENCES [USER](UserID)
);

-- 1.16  RESOURCE
CREATE TABLE RESOURCE (
    ResourceID        INT          NOT NULL IDENTITY(1,1),
    ResourceName      VARCHAR(150) NOT NULL,
    ResourceType      VARCHAR(50)  NOT NULL,
    Unit              VARCHAR(30)  NOT NULL,
    LowStockThreshold INT          NOT NULL DEFAULT 50,
    CONSTRAINT PK_RESOURCE PRIMARY KEY (ResourceID)
);

-- 1.17  INVENTORY
CREATE TABLE INVENTORY (
    WarehouseID        INT      NOT NULL,
    ResourceID         INT      NOT NULL,
    QuantityAvailable  INT      NOT NULL DEFAULT 0,
    QuantityDispatched INT      NOT NULL DEFAULT 0,
    QuantityConsumed   INT      NOT NULL DEFAULT 0,
    LastUpdated        DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_INVENTORY           PRIMARY KEY (WarehouseID, ResourceID),
    CONSTRAINT FK_INVENTORY_WAREHOUSE FOREIGN KEY (WarehouseID) REFERENCES WAREHOUSE(WarehouseID),
    CONSTRAINT FK_INVENTORY_RESOURCE  FOREIGN KEY (ResourceID)  REFERENCES RESOURCE(ResourceID)
);

-- 1.18  APPROVALREQUEST
CREATE TABLE APPROVALREQUEST (
    RequestID   INT            NOT NULL IDENTITY(1,1),
    RequestType VARCHAR(50)    NOT NULL,
    RequestedAt DATETIME       NOT NULL DEFAULT GETDATE(),
    Status      VARCHAR(30)    NOT NULL DEFAULT 'Pending',
    DecisionAt  DATETIME       NULL,
    Remarks     NVARCHAR(MAX)  NULL,
    RequestedBy INT            NOT NULL,
    ApprovedBy  INT            NULL,
    CONSTRAINT PK_APPROVALREQUEST      PRIMARY KEY (RequestID),
    CONSTRAINT FK_APPROVAL_REQUESTEDBY FOREIGN KEY (RequestedBy) REFERENCES [USER](UserID),
    CONSTRAINT FK_APPROVAL_APPROVEDBY  FOREIGN KEY (ApprovedBy)  REFERENCES [USER](UserID)
);

-- 1.19  RESOURCEALLOCATION
CREATE TABLE RESOURCEALLOCATION (
    AllocationID      INT      NOT NULL IDENTITY(1,1),
    QuantityAllocated INT      NOT NULL,
    AllocatedAt       DATETIME NOT NULL DEFAULT GETDATE(),
    ResourceID        INT      NOT NULL,
    WarehouseID       INT      NOT NULL,
    EventID           INT      NULL,
    AllocatedBy       INT      NULL,
    RequestID         INT      NULL,
    ReportID          INT      NULL,
    CONSTRAINT PK_RESOURCEALLOCATION PRIMARY KEY (AllocationID),
    CONSTRAINT FK_ALLOC_RESOURCE     FOREIGN KEY (ResourceID)  REFERENCES RESOURCE(ResourceID),
    CONSTRAINT FK_ALLOC_WAREHOUSE    FOREIGN KEY (WarehouseID) REFERENCES WAREHOUSE(WarehouseID),
    CONSTRAINT FK_ALLOC_EVENT        FOREIGN KEY (EventID)     REFERENCES DISASTEREVENT(EventID),
    CONSTRAINT FK_ALLOC_ALLOCATEDBY  FOREIGN KEY (AllocatedBy) REFERENCES [USER](UserID),
    CONSTRAINT FK_ALLOC_REQUEST      FOREIGN KEY (RequestID)   REFERENCES APPROVALREQUEST(RequestID),
    CONSTRAINT FK_ALLOC_REPORT       FOREIGN KEY (ReportID)    REFERENCES EMERGENCYREPORT(ReportID)
);

-- 1.20  DONOR
CREATE TABLE DONOR (
    DonorID   INT          NOT NULL IDENTITY(1,1),
    Name      VARCHAR(150) NOT NULL,
    DonorType VARCHAR(50)  NOT NULL,
    Email     VARCHAR(150) NULL,
    Phone     VARCHAR(20)  NULL,
    Street    VARCHAR(150) NULL,
    City      VARCHAR(100) NULL,
    Country   VARCHAR(100) NULL,
    CONSTRAINT PK_DONOR PRIMARY KEY (DonorID)
);

-- 1.21  SUPPLIER
CREATE TABLE SUPPLIER (
    SupplierID   INT          NOT NULL IDENTITY(1,1),
    SupplierName VARCHAR(150) NOT NULL,
    Email        VARCHAR(150) NULL,
    Phone        VARCHAR(20)  NULL,
    CONSTRAINT PK_SUPPLIER PRIMARY KEY (SupplierID)
);

-- 1.22  FINANCIALTRANSACTION
CREATE TABLE FINANCIALTRANSACTION (
    TransactionID   INT            NOT NULL IDENTITY(1,1),
    Amount          DECIMAL(15,2)  NOT NULL,
    TransactionType VARCHAR(50)    NOT NULL,
    TransactionDate DATETIME       NOT NULL DEFAULT GETDATE(),
    Description     NVARCHAR(MAX)  NULL,
    Status          VARCHAR(30)    NOT NULL DEFAULT 'Pending',
    ApprovedBy      INT            NULL,
    DonorID         INT            NULL,
    SupplierID      INT            NULL,
    RequestID       INT            NULL,
    ResourceID      INT            NULL,
    EventID         INT            NULL,
    CONSTRAINT PK_FINANCIALTRANSACTION PRIMARY KEY (TransactionID),
    CONSTRAINT FK_TXN_APPROVEDBY       FOREIGN KEY (ApprovedBy)  REFERENCES [USER](UserID),
    CONSTRAINT FK_TXN_DONOR            FOREIGN KEY (DonorID)     REFERENCES DONOR(DonorID),
    CONSTRAINT FK_TXN_SUPPLIER         FOREIGN KEY (SupplierID)  REFERENCES SUPPLIER(SupplierID),
    CONSTRAINT FK_TXN_REQUEST          FOREIGN KEY (RequestID)   REFERENCES APPROVALREQUEST(RequestID),
    CONSTRAINT FK_TXN_RESOURCE         FOREIGN KEY (ResourceID)  REFERENCES RESOURCE(ResourceID),
    CONSTRAINT FK_TXN_EVENT            FOREIGN KEY (EventID)     REFERENCES DISASTEREVENT(EventID)
);

--  SECTION 2: DML – SEED DATA

-- 2.1  ROLES
INSERT INTO ROLE (RoleName) VALUES
    ('Administrator'),
    ('Emergency Operator'),
    ('Field Officer'),
    ('Warehouse Manager'),
    ('Finance Officer');

-- 2.2  USERS
INSERT INTO [USER] (FullName, Email, PasswordHash, PhoneNumber, IsActive, RoleID) VALUES
    ('Admin User',     'admin@disastermis.pk',  CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000001', 1, 1),
    ('Sara Khan',      'sara@disastermis.pk',   CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000002', 1, 2),
    ('Ali Raza',       'ali@disastermis.pk',    CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000003', 1, 3),
    ('Hina Warehouse', 'hina@disastermis.pk',   CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000004', 1, 4),
    ('Zain Finance',   'zain@disastermis.pk',   CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000005', 1, 5),
    ('Maria Operator', 'maria@disastermis.pk',  CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000006', 1, 2),
    ('Omar Field',     'omar@disastermis.pk',   CONVERT(VARCHAR(255), HASHBYTES('SHA2_256', 'Password@123'), 2), '0300-0000007', 1, 3);

-- 2.3  CITIZENS
INSERT INTO CITIZEN (FirstName, LastName, ContactNumber, Street, City, Region) VALUES
    ('Sajjal',  'Ishtiaq', '0333-1111111', 'Block 4 PECHS',  'Karachi',    'Sindh'),
    ('Sana', 'Noor',    '0333-2222222', 'G-10 Markaz',    'Islamabad',  'ICT'),
    ('Ansa',  'Sajid',     '0333-3333333', 'Johar Town',     'Lahore',     'Punjab'),
    ('Alyan', 'Ahmed',     '0333-4444444', 'Hayatabad Ph 1', 'Peshawar',   'KPK'),
    ('Salman',  'Ahmed',   '0333-5555555', 'Satellite Town', 'Quetta',     'Balochistan'),
    ('Minahil',  'Changez',    '0333-6666666', 'Model Colony',   'Karachi',    'Sindh'),
    ('Zarmeen', 'Ahmed',   '0333-7777777', 'Canal Road',     'Faisalabad', 'Punjab');

-- 2.4  DISASTER EVENTS
INSERT INTO DISASTEREVENT (EventName, DisasterType, StartDate, EndDate, AffectedRegion, Status, TotalBudget) VALUES
    ('Karachi Flood 2025', 'Flood',      '2025-07-01', NULL,         'Sindh',       'Active',   5000000.00),
    ('Balochistan Quake',  'Earthquake', '2025-03-15', '2025-04-30', 'Balochistan', 'Resolved', 3000000.00),
    ('Lahore Urban Fire',  'Fire',       '2025-09-10', NULL,         'Punjab',      'Active',   1500000.00),
    ('Peshawar Landslide', 'Landslide',  '2025-08-20', '2025-09-05', 'KPK',         'Resolved',  800000.00),
    ('Islamabad Heatwave', 'Heatwave',   '2025-06-01', '2025-06-30', 'ICT',         'Resolved',  500000.00);

-- 2.5  EMERGENCY REPORTS
INSERT INTO EMERGENCYREPORT (Latitude, Longitude, DisasterType, SeverityLevel, TimeOfReport, Status, Description, CitizenID, EventID) VALUES
    (24.860700, 67.001100, 'Flood',      'Critical', '2025-07-02 08:30:00', 'In Progress', 'Water level rising rapidly in PECHS',       1, 1),
    (30.375300, 69.345100, 'Earthquake', 'High',     '2025-03-15 04:10:00', 'Resolved',    'Buildings collapsed in Quetta outskirts',   5, 2),
    (31.520400, 74.358700, 'Fire',       'Medium',   '2025-09-11 13:00:00', 'In Progress', 'Industrial area fire spreading',            3, 3),
    (34.015100, 71.524900, 'Landslide',  'High',     '2025-08-21 06:00:00', 'Resolved',    'Road blocked due to landslide near Khyber', 4, 4),
    (33.721500, 73.043300, 'Heatwave',   'Low',      '2025-06-02 11:00:00', 'Resolved',    'Mass heat exhaustion in downtown area',     2, 5),
    (24.905600, 67.082200, 'Flood',      'Critical', '2025-07-05 09:00:00', 'Pending',     'Residential area completely submerged',     6, 1),
    (31.450400, 73.135000, 'Fire',       'High',     '2025-09-12 15:30:00', 'Pending',     'Warehouse fire in Faisalabad',              7, 3);

-- 2.6  HOSPITALS
INSERT INTO HOSPITAL (Name, Address, City, Latitude, Longitude, ContactNumber, TotalBeds, EmergencyCapacity) VALUES
    ('Civil Hospital Karachi',    'Karachi Civil Lines', 'Karachi',   24.872600, 67.012400, '021-99215740', 1200, 200),
    ('PIMS Hospital',             'G-8/3, Islamabad',    'Islamabad', 33.717200, 73.061100, '051-9261170',   800, 150),
    ('Mayo Hospital Lahore',      'Nila Gumbad, Lahore', 'Lahore',    31.560000, 74.322800, '042-99231580', 1500, 300),
    ('Hayatabad Medical Complex', 'Hayatabad, Peshawar', 'Peshawar',  34.020000, 71.531000, '091-9217482',   600, 100),
    ('Sandeman Hospital',         'Quetta City',         'Quetta',    30.196000, 67.009000, '081-9201050',   500,  80);

-- 2.7  PATIENTS
INSERT INTO PATIENT (FirstName, LastName, Age, Gender, Condition) VALUES
    ('Tariq',  'Iqbal',   45, 'Male',   'Fracture'),
    ('Sana',   'Hussain', 30, 'Female', 'Dehydration'),
    ('Kamran', 'Abbas',   60, 'Male',   'Cardiac Arrest'),
    ('Lubna',  'Qureshi', 25, 'Female', 'Burns 2nd Degree'),
    ('Hamza',  'Mirza',   18, 'Male',   'Head Trauma'),
    ('Rabia',  'Ansari',  50, 'Female', 'Heat Stroke');

-- 2.8  PATIENT ADMISSIONS
INSERT INTO PATIENTADMISSION (PatientID, HospitalID, ReportID, AssignedBy, AdmissionTime) VALUES
    (1, 1, 2, 2, '2025-03-15 10:00:00'),
    (2, 1, 1, 2, '2025-07-02 10:30:00'),
    (3, 3, 3, 6, '2025-09-11 14:00:00'),
    (4, 4, 4, 2, '2025-08-21 08:00:00'),
    (5, 5, 2, 6, '2025-03-15 11:00:00'),
    (6, 2, 5, 2, '2025-06-02 13:00:00');

-- 2.9  RESCUE TEAMS
INSERT INTO RESCUETEAM (TeamName, TeamType, Latitude, Longitude, AvailabilityStatus, ContactNumber, Capacity) VALUES
    ('Alpha Rescue', 'Rescue',  24.860700, 67.001100, 'Available', '0311-1000001', 12),
    ('Beta Medical', 'Medical', 30.375300, 69.345100, 'Assigned',  '0311-1000002',  8),
    ('Gamma Fire',   'Fire',    31.520400, 74.358700, 'Busy',      '0311-1000003', 10),
    ('Delta Search', 'Rescue',  34.015100, 71.524900, 'Available', '0311-1000004', 15),
    ('Epsilon Med',  'Medical', 33.721500, 73.043300, 'Available', '0311-1000005',  6);

-- 2.10  TEAM MEMBERS
INSERT INTO TEAMMEMBER (FirstName, LastName, Designation, PhoneNumber, TeamID) VALUES
    ('Shahid', 'Ali',      'Team Leader',   '0320-1111101', 1),
    ('Nadia',  'Baig',     'Paramedic',     '0320-1111102', 2),
    ('Fawad',  'Chaudhry', 'Firefighter',   '0320-1111103', 3),
    ('Rana',   'Younas',   'Rescue Diver',  '0320-1111104', 1),
    ('Hira',   'Saleem',   'Nurse',         '0320-1111105', 2),
    ('Asad',   'Nawaz',    'Firefighter',   '0320-1111106', 3),
    ('Zara',   'Farooq',   'Search Expert', '0320-1111107', 4),
    ('Junaid', 'Khan',     'Doctor',        '0320-1111108', 5);

-- 2.11  SPECIALIZATIONS
INSERT INTO SPECIALIZATION (MemberID, Specialization) VALUES
    (1, 'Water Rescue'),
    (1, 'First Aid'),
    (2, 'Emergency Medicine'),
    (3, 'Hazmat'),
    (4, 'Underwater Rescue'),
    (5, 'Trauma Care'),
    (6, 'Structural Fire'),
    (7, 'K9 Search and Rescue'),
    (8, 'Surgery');

-- 2.12  TEAM ASSIGNMENTS
INSERT INTO TEAMASSIGNMENT (TeamID, ReportID, AssignedBy, AssignedAt) VALUES
    (1, 1, 2, '2025-07-02 09:00:00'),
    (2, 2, 2, '2025-03-15 05:00:00'),
    (3, 3, 6, '2025-09-11 13:30:00'),
    (4, 4, 2, '2025-08-21 06:30:00'),
    (5, 5, 6, '2025-06-02 11:30:00');

-- 2.13  TEAM ACTIVITY LOGS
INSERT INTO TEAMACTIVITYLOG (TeamID, StartTime, Status, OutcomeSummary, EndTime) VALUES
    (1, '2025-07-02 09:00:00', 'In Progress', NULL,                              NULL),
    (2, '2025-03-15 05:00:00', 'Completed',   '150 survivors evacuated',         '2025-03-20 18:00:00'),
    (3, '2025-09-11 13:30:00', 'In Progress', NULL,                              NULL),
    (4, '2025-08-21 06:30:00', 'Completed',   'Road cleared after 6 hours',      '2025-08-21 12:30:00'),
    (5, '2025-06-02 11:30:00', 'Completed',   '80 patients treated for heatstroke','2025-06-02 22:00:00');

-- 2.14  WAREHOUSES
INSERT INTO WAREHOUSE (Name, Address, City, Latitude, Longitude, Capacity, ManagerID) VALUES
    ('Karachi Central Store', 'Korangi Industrial Area',  'Karachi',   24.820000, 67.090000, 5000, 4),
    ('Lahore Hub',            'Sundar Industrial Estate', 'Lahore',    31.480000, 74.290000, 4000, 4),
    ('Islamabad Reserve',     'I-9 Industrial Zone',      'Islamabad', 33.670000, 73.070000, 3000, 4),
    ('Quetta Depot',          'Quetta City',              'Quetta',    30.210000, 67.000000, 2000, 4),
    ('Peshawar Store',        'Ring Road Area',           'Peshawar',  34.010000, 71.550000, 2500, 4);

-- 2.15  RESOURCES
INSERT INTO RESOURCE (ResourceName, ResourceType, Unit, LowStockThreshold) VALUES
    ('Rice Bags',         'Food',      'Kg',    500),
    ('Bottled Water',     'Water',     'Liter', 1000),
    ('Paracetamol 500mg', 'Medicine',  'Box',   200),
    ('Tent 4-person',     'Shelter',   'Unit',   50),
    ('Blankets',          'Shelter',   'Unit',  100),
    ('ORS Sachets',       'Medicine',  'Box',   300),
    ('Diesel Generator',  'Equipment', 'Liter', 500),
    ('First Aid Kit',     'Medicine',  'Kit',   100);

-- 2.16  INVENTORY
INSERT INTO INVENTORY (WarehouseID, ResourceID, QuantityAvailable, QuantityDispatched, QuantityConsumed) VALUES
    (1, 1, 2000, 300, 200),
    (1, 2, 5000, 500, 400),
    (1, 3,  800, 100,  80),
    (1, 4,  120,  20,  15),
    (2, 1, 1500, 200, 100),
    (2, 2, 3000, 300, 200),
    (2, 5,  400,  50,  30),
    (3, 6, 1000, 150, 100),
    (3, 7, 2000, 200, 180),
    (4, 8,  300,  40,  30),
    (5, 1,  800, 100,  80),
    (5, 3,  400,  60,  40);

-- 2.17  APPROVAL REQUESTS
INSERT INTO APPROVALREQUEST (RequestType, Status, RequestedBy, ApprovedBy, DecisionAt, Remarks) VALUES
    ('Resource Distribution', 'Approved', 3, 1, '2025-07-03 10:00:00', 'Approved for flood relief'),
    ('Rescue Deployment',     'Approved', 2, 1, '2025-03-15 05:30:00', 'Urgent earthquake response'),
    ('Financial Approval',    'Pending',  5, NULL, NULL,               NULL),
    ('Resource Distribution', 'Rejected', 3, 1, '2025-09-12 09:00:00', 'Insufficient stock in region'),
    ('Rescue Deployment',     'Approved', 6, 1, '2025-09-11 13:00:00', 'Fire emergency confirmed');

-- 2.18  RESOURCE ALLOCATIONS
INSERT INTO RESOURCEALLOCATION (QuantityAllocated, ResourceID, WarehouseID, EventID, AllocatedBy, RequestID, ReportID) VALUES
    (500,  1, 1, 1, 3, 1, 1),
    (1000, 2, 1, 1, 3, 1, 1),
    (200,  3, 1, 2, 3, 2, 2),
    ( 50,  4, 1, 1, 4, 1, 6),
    (100,  5, 2, 3, 4, 5, 3);

-- 2.19  DONORS
INSERT INTO DONOR (Name, DonorType, Email, Phone, Street, City, Country) VALUES
    ('Hashoo Foundation',    'Organization', 'donations@hashoo.org', '021-35630074', 'M.T. Khan Road',     'Karachi',   'Pakistan'),
    ('Alkhidmat Foundation', 'Organization', 'info@alkhidmat.org',   '042-35761999', 'Garden Town',        'Lahore',    'Pakistan'),
    ('Muhammad Tahir',       'Individual',   'mtahir@gmail.com',     '0321-5556677', 'F-8/2',              'Islamabad', 'Pakistan'),
    ('USAID Pakistan',       'Organization', 'pkinfo@usaid.gov',     '051-2087300',  'Diplomatic Enclave', 'Islamabad', 'Pakistan'),
    ('Imran Nazar',          'Individual',   'inazar@hotmail.com',   '0311-4441122', 'Clifton Block 9',    'Karachi',   'Pakistan');

-- 2.20  SUPPLIERS
INSERT INTO SUPPLIER (SupplierName, Email, Phone) VALUES
    ('National Logistics Cell', 'info@nlc.pk',            '051-9270900'),
    ('Engro Foods Ltd',         'supply@engro.com',       '021-35296800'),
    ('Herbion Pharma',          'orders@herbion.com',     '042-35976000'),
    ('Metro Cash and Carry',    'corporate@metro.com.pk', '021-32456789'),
    ('PSO Fuel Services',       'fuel@pso.com.pk',        '021-32068000');

-- 2.21  FINANCIAL TRANSACTIONS
INSERT INTO FINANCIALTRANSACTION
    (Amount, TransactionType, TransactionDate, Description, Status, ApprovedBy, DonorID, SupplierID, RequestID, ResourceID, EventID)
VALUES
    (500000.00, 'Donation',    '2025-07-05 10:00:00', 'Flood relief donation from Hashoo',        'Approved', 1, 1,    NULL, NULL, NULL, 1),
    (250000.00, 'Donation',    '2025-07-06 11:00:00', 'Donation from USAID for Karachi Flood',    'Approved', 1, 4,    NULL, NULL, NULL, 1),
    (180000.00, 'Expense',     '2025-07-07 09:00:00', 'Procurement of food supplies',             'Approved', 5, NULL, 2,    1,   1,    1),
    ( 95000.00, 'Expense',     '2025-03-16 08:00:00', 'Medical supplies for earthquake',          'Approved', 5, NULL, 3,    2,   3,    2),
    (320000.00, 'Donation',    '2025-03-17 12:00:00', 'Alkhidmat donation for earthquake relief', 'Approved', 1, 2,    NULL, NULL, NULL, 2),
    ( 75000.00, 'Procurement', '2025-09-12 10:00:00', 'Fire-fighting equipment purchase',         'Pending',  NULL, NULL, 4,  5,   7,    3),
    ( 50000.00, 'Donation',    '2025-06-03 14:00:00', 'Individual donation for heatwave response','Approved', 1, 5,    NULL, NULL, NULL, 5),
    (120000.00, 'Expense',     '2025-07-10 15:00:00', 'Tent and shelter procurement',             'Approved', 5, NULL, 1,    1,   4,    1);


--  SECTION 3: TRIGGERS

-- 3.1  Prevent negative inventory BEFORE allocation insert
GO
CREATE TRIGGER trg_prevent_negative_inventory
ON RESOURCEALLOCATION
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN INVENTORY inv
            ON inv.WarehouseID = i.WarehouseID
           AND inv.ResourceID  = i.ResourceID
        WHERE inv.QuantityAvailable < i.QuantityAllocated
    )
    BEGIN
        RAISERROR ('Insufficient stock: allocation quantity exceeds available inventory.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    INSERT INTO RESOURCEALLOCATION
        (QuantityAllocated, AllocatedAt, ResourceID, WarehouseID, EventID, AllocatedBy, RequestID, ReportID)
    SELECT
        QuantityAllocated, AllocatedAt, ResourceID, WarehouseID, EventID, AllocatedBy, RequestID, ReportID
    FROM inserted;
END;
GO

-- 3.2  After Resource Allocation: deduct inventory + audit + low-stock alert
CREATE TRIGGER trg_after_resource_allocation
ON RESOURCEALLOCATION
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE inv
    SET inv.QuantityDispatched = inv.QuantityDispatched + i.QuantityAllocated,
        inv.QuantityAvailable  = inv.QuantityAvailable  - i.QuantityAllocated,
        inv.LastUpdated        = GETDATE()
    FROM INVENTORY inv
    JOIN inserted i
        ON inv.WarehouseID = i.WarehouseID
       AND inv.ResourceID  = i.ResourceID;

    INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
    SELECT
        'ALLOCATION',
        'INVENTORY',
        CONCAT('ResourceID=', i.ResourceID, ' WarehouseID=', i.WarehouseID),
        CONCAT('Dispatched +', i.QuantityAllocated, ' | Remaining=', inv.QuantityAvailable)
    FROM inserted i
    JOIN INVENTORY inv
        ON inv.WarehouseID = i.WarehouseID
       AND inv.ResourceID  = i.ResourceID;

    INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
    SELECT
        'LOW_STOCK_ALERT',
        'INVENTORY',
        CONCAT('ResourceID=', i.ResourceID),
        CONCAT('Available=', inv.QuantityAvailable, ' BELOW THRESHOLD')
    FROM inserted i
    JOIN INVENTORY inv
        ON inv.WarehouseID = i.WarehouseID
       AND inv.ResourceID  = i.ResourceID
    JOIN RESOURCE r
        ON r.ResourceID = i.ResourceID
    WHERE inv.QuantityAvailable < r.LowStockThreshold;
END;
GO

-- 3.3  After TeamAssignment INSERT: set team status to Assigned
CREATE TRIGGER trg_team_assignment_status
ON TEAMASSIGNMENT
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE rt
    SET rt.AvailabilityStatus = 'Assigned'
    FROM RESCUETEAM rt
    JOIN inserted i ON rt.TeamID = i.TeamID;

    INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
    SELECT
        'TEAM_ASSIGNED',
        'RESCUETEAM',
        CONCAT('TeamID=', i.TeamID, ' Status=Available'),
        CONCAT('TeamID=', i.TeamID, ' Status=Assigned')
    FROM inserted i;
END;
GO

-- 3.4  After TeamActivityLog UPDATE to Completed: set team to Available
CREATE TRIGGER trg_team_activity_complete
ON TEAMACTIVITYLOG
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(Status)
    BEGIN
        UPDATE rt
        SET rt.AvailabilityStatus = 'Available'
        FROM RESCUETEAM rt
        JOIN inserted i ON rt.TeamID    = i.TeamID
        JOIN deleted  d ON d.ActivityID = i.ActivityID
        WHERE i.Status = 'Completed'
          AND d.Status <> 'Completed';

        INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
        SELECT
            'TEAM_COMPLETED',
            'RESCUETEAM',
            CONCAT('TeamID=', d.TeamID, ' Status=', d.Status),
            CONCAT('TeamID=', i.TeamID, ' Status=Available')
        FROM inserted i
        JOIN deleted  d ON d.ActivityID = i.ActivityID
        WHERE i.Status = 'Completed'
          AND d.Status <> 'Completed';
    END;
END;
GO

-- 3.5  After FinancialTransaction INSERT: audit + update event budget
CREATE TRIGGER trg_financial_transaction_audit
ON FINANCIALTRANSACTION
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
    SELECT
        'FINANCIAL_TXN',
        'FINANCIALTRANSACTION',
        NULL,
        CONCAT('Type=', i.TransactionType,
               ' Amount=', CAST(i.Amount AS VARCHAR(20)),
               ' EventID=', ISNULL(CAST(i.EventID AS VARCHAR(10)), 'N/A'),
               ' Status=', i.Status)
    FROM inserted i;

    UPDATE e
    SET e.TotalBudget = e.TotalBudget + i.Amount
    FROM DISASTEREVENT e
    JOIN inserted i
        ON e.EventID = i.EventID
    WHERE i.TransactionType = 'Donation'
      AND i.Status          = 'Approved'
      AND i.EventID IS NOT NULL;
END;
GO

-- 3.6  After ApprovalRequest UPDATE: audit status change
CREATE TRIGGER trg_approval_status_change
ON APPROVALREQUEST
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(Status)
    BEGIN
        INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
        SELECT
            'APPROVAL_UPDATE',
            'APPROVALREQUEST',
            CONCAT('RequestID=', d.RequestID, ' Status=', d.Status),
            CONCAT('RequestID=', i.RequestID, ' Status=', i.Status,
                   ' DecisionAt=', ISNULL(CONVERT(VARCHAR(20), i.DecisionAt, 120), 'NULL'))
        FROM inserted i
        JOIN deleted  d ON d.RequestID = i.RequestID
        WHERE i.Status <> d.Status;
    END;
END;
GO

-- 3.7  After PatientAdmission INSERT: audit log
CREATE TRIGGER trg_patient_admission_log
ON PATIENTADMISSION
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO AUDITLOG (ActionType, TableAffected, OldValue, NewValue)
    SELECT
        'PATIENT_ADMITTED',
        'PATIENTADMISSION',
        NULL,
        CONCAT('PatientID=', i.PatientID,
               ' HospitalID=', i.HospitalID,
               ' AdmissionTime=', CONVERT(VARCHAR(20), i.AdmissionTime, 120))
    FROM inserted i;
END;
GO

--  SECTION 4: VIEWS
-- 4.1  Active Emergency Reports (Emergency Operators)

CREATE VIEW vw_active_emergency_reports AS
SELECT
    r.ReportID,
    r.DisasterType,
    r.SeverityLevel,
    r.TimeOfReport,
    r.Status,
    r.Description,
    r.Latitude,
    r.Longitude,
    c.FirstName + ' ' + c.LastName AS ReportedBy,
    c.ContactNumber,
    e.EventName
FROM EMERGENCYREPORT r
LEFT JOIN CITIZEN       c ON c.CitizenID = r.CitizenID
LEFT JOIN DISASTEREVENT e ON e.EventID   = r.EventID
WHERE r.Status IN ('Pending', 'In Progress');
GO

-- 4.2  Inventory Summary (Warehouse Managers)

CREATE VIEW vw_inventory_summary AS
SELECT
    w.WarehouseID,
    w.Name                                                    AS WarehouseName,
    w.City,
    res.ResourceID,
    res.ResourceName,
    res.ResourceType,
    res.Unit,
    inv.QuantityAvailable,
    inv.QuantityDispatched,
    inv.QuantityConsumed,
    res.LowStockThreshold,
    CASE
        WHEN inv.QuantityAvailable < res.LowStockThreshold THEN 'LOW STOCK'
        ELSE 'OK'
    END                                                       AS StockStatus,
    inv.LastUpdated
FROM INVENTORY inv
JOIN WAREHOUSE  w   ON w.WarehouseID  = inv.WarehouseID
JOIN RESOURCE   res ON res.ResourceID = inv.ResourceID;
GO

-- 4.3  Financial Summary (Finance Officers)

CREATE VIEW vw_financial_summary AS
SELECT
    t.TransactionID,
    t.TransactionType,
    t.Amount,
    t.TransactionDate,
    t.Status,
    t.Description,
    d.Name        AS DonorName,
    d.DonorType,
    s.SupplierName,
    e.EventName,
    res.ResourceName,
    u.FullName    AS ApprovedByUser
FROM FINANCIALTRANSACTION t
LEFT JOIN DONOR           d   ON d.DonorID      = t.DonorID
LEFT JOIN SUPPLIER        s   ON s.SupplierID   = t.SupplierID
LEFT JOIN DISASTEREVENT   e   ON e.EventID      = t.EventID
LEFT JOIN RESOURCE        res ON res.ResourceID = t.ResourceID
LEFT JOIN [USER]          u   ON u.UserID       = t.ApprovedBy;
GO

-- 4.4  Team Status Overview (Field Officers)

CREATE VIEW vw_team_status AS
SELECT
    rt.TeamID,
    rt.TeamName,
    rt.TeamType,
    rt.AvailabilityStatus,
    rt.Capacity,
    rt.ContactNumber,
    COUNT(tm.MemberID) AS TotalMembers,
    rt.Latitude,
    rt.Longitude
FROM RESCUETEAM  rt
LEFT JOIN TEAMMEMBER tm ON tm.TeamID = rt.TeamID
GROUP BY
    rt.TeamID, rt.TeamName, rt.TeamType, rt.AvailabilityStatus,
    rt.Capacity, rt.ContactNumber, rt.Latitude, rt.Longitude;
GO

-- 4.5  Hospital Capacity Overview
SET STATISTICS TIME ON;

CREATE VIEW vw_hospital_capacity AS
SELECT
    h.HospitalID,
    h.Name              AS HospitalName,
    h.City,
    h.TotalBeds,
    h.EmergencyCapacity,
    COUNT(pa.AdmissionID)               AS CurrentAdmissions,
    h.TotalBeds - COUNT(pa.AdmissionID) AS BedsAvailable
FROM HOSPITAL h
LEFT JOIN PATIENTADMISSION pa
       ON pa.HospitalID    = h.HospitalID
      AND pa.DischargeTime IS NULL
GROUP BY h.HospitalID, h.Name, h.City, h.TotalBeds, h.EmergencyCapacity;
GO

-- 4.6  Pending Approvals (Administrators)
CREATE VIEW vw_pending_approvals AS
SELECT
    ar.RequestID,
    ar.RequestType,
    ar.RequestedAt,
    ar.Status,
    ar.Remarks,
    ru.FullName AS RequestedByUser,
    au.FullName AS ApprovedByUser
FROM APPROVALREQUEST ar
JOIN      [USER] ru ON ru.UserID = ar.RequestedBy
LEFT JOIN [USER] au ON au.UserID = ar.ApprovedBy
WHERE ar.Status = 'Pending';
GO

-- 4.7  Event Budget vs Expenditure
CREATE VIEW vw_event_budget_analysis AS
SELECT
    e.EventID,
    e.EventName,
    e.DisasterType,
    e.Status,
    e.TotalBudget,
    ISNULL(SUM(CASE WHEN t.TransactionType = 'Donation'
                     AND t.Status = 'Approved' THEN t.Amount ELSE 0 END), 0)
        AS TotalDonations,
    ISNULL(SUM(CASE WHEN t.TransactionType IN ('Expense','Procurement')
                     AND t.Status = 'Approved' THEN t.Amount ELSE 0 END), 0)
        AS TotalExpenses,
    e.TotalBudget -
    ISNULL(SUM(CASE WHEN t.TransactionType IN ('Expense','Procurement')
                     AND t.Status = 'Approved' THEN t.Amount ELSE 0 END), 0)
        AS RemainingBudget
FROM DISASTEREVENT e
LEFT JOIN FINANCIALTRANSACTION t ON t.EventID = e.EventID
GROUP BY e.EventID, e.EventName, e.DisasterType, e.Status, e.TotalBudget;
GO
-- 4.8  Audit Trail (Administrators)

CREATE VIEW vw_audit_trail AS
SELECT
    al.LogID,
    al.ActionType,
    al.TableAffected,
    al.OldValue,
    al.NewValue,
    al.LogTimestamp,
    u.FullName AS PerformedBy
FROM AUDITLOG al
LEFT JOIN [USER] u ON u.UserID = al.UserID;
GO

--  SECTION 5: INDEXES

CREATE INDEX IX_Report_DisasterType  ON EMERGENCYREPORT (DisasterType);
CREATE INDEX IX_Report_SeverityLevel ON EMERGENCYREPORT (SeverityLevel);
CREATE INDEX IX_Report_Status        ON EMERGENCYREPORT (Status);
CREATE INDEX IX_Report_TimeOfReport  ON EMERGENCYREPORT (TimeOfReport);
CREATE INDEX IX_Report_Type_Severity ON EMERGENCYREPORT (DisasterType, SeverityLevel);

CREATE INDEX IX_Txn_TransactionDate  ON FINANCIALTRANSACTION (TransactionDate);
CREATE INDEX IX_Txn_TransactionType  ON FINANCIALTRANSACTION (TransactionType);
CREATE INDEX IX_Txn_EventID          ON FINANCIALTRANSACTION (EventID);
CREATE INDEX IX_Txn_Status           ON FINANCIALTRANSACTION (Status);
CREATE INDEX IX_Txn_Type_Status      ON FINANCIALTRANSACTION (TransactionType, Status);

CREATE INDEX IX_Alloc_ResourceID     ON RESOURCEALLOCATION (ResourceID);
CREATE INDEX IX_Alloc_WarehouseID    ON RESOURCEALLOCATION (WarehouseID);
CREATE INDEX IX_Alloc_EventID        ON RESOURCEALLOCATION (EventID);
CREATE INDEX IX_Alloc_AllocatedAt    ON RESOURCEALLOCATION (AllocatedAt);

CREATE INDEX IX_Resource_Type        ON RESOURCE (ResourceType);

CREATE INDEX IX_Assignment_TeamID    ON TEAMASSIGNMENT (TeamID);
CREATE INDEX IX_Assignment_ReportID  ON TEAMASSIGNMENT (ReportID);

CREATE INDEX IX_Inventory_Available  ON INVENTORY (WarehouseID, QuantityAvailable);

CREATE INDEX IX_AuditLog_Timestamp   ON AUDITLOG (LogTimestamp);
CREATE INDEX IX_AuditLog_ActionType  ON AUDITLOG (ActionType);

CREATE INDEX IX_Admission_HospitalID    ON PATIENTADMISSION (HospitalID);
CREATE INDEX IX_Admission_DischargeTime ON PATIENTADMISSION (DischargeTime);

CREATE INDEX IX_Approval_Status      ON APPROVALREQUEST (Status);
CREATE INDEX IX_Approval_RequestType ON APPROVALREQUEST (RequestType);
GO

--  SECTION 6: STORED PROCEDURES

-- SP-1: Allocate Resources (ACID-safe)
CREATE PROCEDURE sp_allocate_resource
    @resource_id  INT,
    @warehouse_id INT,
    @event_id     INT,
    @qty          INT,
    @allocated_by INT,
    @request_id   INT,
    @report_id    INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @available INT = 0;

    BEGIN TRANSACTION;

        SELECT @available = QuantityAvailable
        FROM INVENTORY WITH (UPDLOCK, ROWLOCK)
        WHERE WarehouseID = @warehouse_id
          AND ResourceID  = @resource_id;

        IF @available < @qty
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR ('Allocation failed: insufficient stock.', 16, 1);
            RETURN;
        END;

        INSERT INTO RESOURCEALLOCATION
            (QuantityAllocated, ResourceID, WarehouseID, EventID, AllocatedBy, RequestID, ReportID)
        VALUES
            (@qty, @resource_id, @warehouse_id, @event_id, @allocated_by, @request_id, @report_id);

    COMMIT TRANSACTION;
END;
GO

-- SP-2: Assign Rescue Team (ACID-safe)
CREATE PROCEDURE sp_assign_rescue_team
    @team_id     INT,
    @report_id   INT,
    @assigned_by INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @status VARCHAR(30);

    BEGIN TRANSACTION;

        SELECT @status = AvailabilityStatus
        FROM RESCUETEAM WITH (UPDLOCK, ROWLOCK)
        WHERE TeamID = @team_id;

        IF @status <> 'Available'
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR ('Team is not currently available for assignment.', 16, 1);
            RETURN;
        END;

        INSERT INTO TEAMASSIGNMENT (TeamID, ReportID, AssignedBy)
        VALUES (@team_id, @report_id, @assigned_by);

    COMMIT TRANSACTION;
END;
GO

-- SP-3: Record Financial Transaction with Approval Check
CREATE PROCEDURE sp_record_financial_transaction
    @amount      DECIMAL(15,2),
    @type        VARCHAR(50),
    @description NVARCHAR(MAX),
    @donor_id    INT,
    @supplier_id INT,
    @event_id    INT,
    @resource_id INT,
    @approved_by INT,
    @request_id  INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @status VARCHAR(30) = 'Pending';

    IF @approved_by IS NOT NULL
        SET @status = 'Approved';

    BEGIN TRANSACTION;

        INSERT INTO FINANCIALTRANSACTION
            (Amount, TransactionType, Description, Status, ApprovedBy,
             DonorID, SupplierID, EventID, ResourceID, RequestID)
        VALUES
            (@amount, @type, @description, @status, @approved_by,
             @donor_id, @supplier_id, @event_id, @resource_id, @request_id);

    COMMIT TRANSACTION;
END;
GO

-- SP-4: Approve or Reject a Request
CREATE PROCEDURE sp_process_approval
    @request_id  INT,
    @approver_id INT,
    @decision    VARCHAR(20),
    @remarks     NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

        UPDATE APPROVALREQUEST
        SET Status     = @decision,
            ApprovedBy = @approver_id,
            DecisionAt = GETDATE(),
            Remarks    = @remarks
        WHERE RequestID = @request_id
          AND Status    = 'Pending';

        IF @@ROWCOUNT = 0
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR ('Request not found or already processed.', 16, 1);
            RETURN;
        END;

    COMMIT TRANSACTION;
END;
GO

-- SP-5: Admit Patient (with hospital capacity guard)
CREATE PROCEDURE sp_admit_patient
    @patient_id  INT,
    @hospital_id INT,
    @report_id   INT,
    @assigned_by INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @capacity INT;
    DECLARE @admitted INT;

    BEGIN TRANSACTION;

        SELECT @capacity = EmergencyCapacity
        FROM HOSPITAL WITH (UPDLOCK, ROWLOCK)
        WHERE HospitalID = @hospital_id;

        SELECT @admitted = COUNT(*)
        FROM PATIENTADMISSION
        WHERE HospitalID    = @hospital_id
          AND DischargeTime IS NULL;

        IF @admitted >= @capacity
        BEGIN
            ROLLBACK TRANSACTION;
            RAISERROR ('Hospital emergency capacity reached.', 16, 1);
            RETURN;
        END;

        INSERT INTO PATIENTADMISSION (PatientID, HospitalID, ReportID, AssignedBy)
        VALUES (@patient_id, @hospital_id, @report_id, @assigned_by);

    COMMIT TRANSACTION;
END;
GO

--Direct Query & Query via view 
SET STATISTICS TIME ON;

-- Using View
SELECT * FROM vw_active_emergency_reports;

-- Direct Query
SELECT
    r.ReportID,
    r.DisasterType,
    r.SeverityLevel,
    r.TimeOfReport,
    r.Status,
    r.Description,
    r.Latitude,
    r.Longitude,
    c.FirstName + ' ' + c.LastName AS ReportedBy,
    c.ContactNumber,
    e.EventName
FROM EMERGENCYREPORT r
LEFT JOIN CITIZEN       c ON c.CitizenID = r.CitizenID
LEFT JOIN DISASTEREVENT e ON e.EventID   = r.EventID
WHERE r.Status IN ('Pending', 'In Progress');

SET STATISTICS TIME OFF;
--  SECTION 7: MIS REPORTING QUERIES

-- Q1: Incident count by DisasterType and SeverityLevel
SELECT
    DisasterType,
    SeverityLevel,
    COUNT(*) AS TotalReports
FROM EMERGENCYREPORT
GROUP BY DisasterType, SeverityLevel
ORDER BY TotalReports DESC;

-- Q2: Resource utilisation per warehouse (via view)
SELECT *
FROM vw_inventory_summary
ORDER BY StockStatus DESC, WarehouseName;

-- Q3: Team response times (Assignment to Completion)
SELECT
    rt.TeamName,
    rt.TeamType,
    ta.AssignedAt,
    tal.EndTime,
    DATEDIFF(HOUR, ta.AssignedAt, tal.EndTime) AS ResponseHours,
    tal.Status,
    tal.OutcomeSummary
FROM TEAMASSIGNMENT ta
JOIN RESCUETEAM      rt  ON rt.TeamID  = ta.TeamID
JOIN TEAMACTIVITYLOG tal ON tal.TeamID = ta.TeamID
ORDER BY ResponseHours;

-- Q4: Financial summary by event
SELECT * FROM vw_event_budget_analysis
ORDER BY TotalExpenses DESC;

-- Q5: Top donors by total approved donations
SELECT
    d.Name,
    d.DonorType,
    SUM(t.Amount) AS TotalDonated
FROM FINANCIALTRANSACTION t
JOIN DONOR d ON d.DonorID = t.DonorID
WHERE t.TransactionType = 'Donation'
  AND t.Status          = 'Approved'
GROUP BY d.DonorID, d.Name, d.DonorType
ORDER BY TotalDonated DESC;

-- Q6: Low-stock resources across all warehouses
SELECT *
FROM vw_inventory_summary
WHERE StockStatus = 'LOW STOCK'
ORDER BY QuantityAvailable ASC;

-- Q7: Hospital bed availability
SELECT * FROM vw_hospital_capacity
ORDER BY BedsAvailable ASC;

-- Q8: Emergency reports per region and city
SELECT
    c.Region,
    c.City,
    COUNT(r.ReportID)                                               AS Reports,
    SUM(CASE WHEN r.SeverityLevel = 'Critical' THEN 1 ELSE 0 END)  AS CriticalCount
FROM EMERGENCYREPORT r
JOIN CITIZEN c ON c.CitizenID = r.CitizenID
GROUP BY c.Region, c.City
ORDER BY CriticalCount DESC;

-- Q9: Approval workflow status summary
SELECT
    RequestType,
    Status,
    COUNT(*) AS Total
FROM APPROVALREQUEST
GROUP BY RequestType, Status
ORDER BY RequestType;

-- Q10a: Active reports via VIEW (role-safe, abstracted)
SELECT * FROM vw_active_emergency_reports
ORDER BY SeverityLevel;

-- Q10b: Active reports via direct base tables (latency comparison)
SELECT
    r.ReportID,
    r.DisasterType,
    r.SeverityLevel,
    r.TimeOfReport,
    r.Status,
    r.Description,
    r.Latitude,
    r.Longitude,
    c.FirstName + ' ' + c.LastName AS ReportedBy,
    c.ContactNumber,
    e.EventName
FROM EMERGENCYREPORT r
LEFT JOIN CITIZEN       c ON c.CitizenID = r.CitizenID
LEFT JOIN DISASTEREVENT e ON e.EventID   = r.EventID
WHERE r.Status IN ('Pending', 'In Progress')
ORDER BY r.SeverityLevel;

-- Q11: Audit trail - most recent 50 entries
SELECT TOP 50 * FROM vw_audit_trail
ORDER BY LogTimestamp DESC;

-- Q12: Index usage verification (run with Actual Execution Plan in SSMS via Ctrl+M)
SELECT * FROM EMERGENCYREPORT
WHERE DisasterType = 'Flood' AND SeverityLevel = 'Critical';

SELECT * FROM FINANCIALTRANSACTION
WHERE TransactionDate BETWEEN '2025-07-01' AND '2025-07-31'
  AND TransactionType = 'Donation';

-- Q13: Rescue team availability dashboard
SELECT * FROM vw_team_status
ORDER BY AvailabilityStatus;

-- Q14: Month-wise transaction volume
SELECT
    FORMAT(TransactionDate, 'yyyy-MM') AS Month,
    TransactionType,
    COUNT(*)                           AS TxnCount,
    SUM(Amount)                        AS TotalAmount
FROM FINANCIALTRANSACTION
WHERE Status = 'Approved'
GROUP BY FORMAT(TransactionDate, 'yyyy-MM'), TransactionType
ORDER BY Month;

-- Q15: Team members with all specializations
SELECT
    rt.TeamName,
    rt.TeamType,
    tm.FirstName + ' ' + tm.LastName              AS MemberName,
    tm.Designation,
    STRING_AGG(sp.Specialization, ', ')
        WITHIN GROUP (ORDER BY sp.Specialization)  AS Specializations
FROM TEAMMEMBER  tm
JOIN RESCUETEAM  rt ON rt.TeamID   = tm.TeamID
LEFT JOIN SPECIALIZATION sp ON sp.MemberID = tm.MemberID
GROUP BY rt.TeamName, rt.TeamType, tm.MemberID, tm.FirstName, tm.LastName, tm.Designation
ORDER BY rt.TeamName, tm.LastName;

-- Q16: Pending approvals with requester details
SELECT * FROM vw_pending_approvals;

-- Q17: Overall MIS dashboard summary (single-row snapshot)
SELECT
    (SELECT COUNT(*) FROM EMERGENCYREPORT WHERE Status = 'Pending')                         AS PendingReports,
    (SELECT COUNT(*) FROM EMERGENCYREPORT WHERE Status = 'In Progress')                     AS ActiveReports,
    (SELECT COUNT(*) FROM RESCUETEAM      WHERE AvailabilityStatus = 'Available')           AS TeamsAvailable,
    (SELECT COUNT(*) FROM RESCUETEAM      WHERE AvailabilityStatus = 'Assigned')            AS TeamsAssigned,
    (SELECT COUNT(*) FROM APPROVALREQUEST WHERE Status = 'Pending')                         AS PendingApprovals,
    (SELECT SUM(Amount) FROM FINANCIALTRANSACTION
        WHERE TransactionType = 'Donation' AND Status = 'Approved')                         AS TotalDonationsReceived,
    (SELECT SUM(Amount) FROM FINANCIALTRANSACTION
        WHERE TransactionType IN ('Expense','Procurement') AND Status = 'Approved')         AS TotalExpensesIncurred;

      

--View 1
 SET STATISTICS TIME ON;
-- View
SELECT * FROM vw_active_emergency_reports;

-- Direct Query
SELECT
    r.ReportID,
    r.DisasterType,
    r.SeverityLevel,
    r.TimeOfReport,
    r.Status,
    r.Description,
    r.Latitude,
    r.Longitude,
    c.FirstName + ' ' + c.LastName AS ReportedBy,
    c.ContactNumber,
    e.EventName
FROM EMERGENCYREPORT r
LEFT JOIN CITIZEN c ON c.CitizenID = r.CitizenID
LEFT JOIN DISASTEREVENT e ON e.EventID = r.EventID
WHERE r.Status IN ('Pending', 'In Progress');

--View 2
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_inventory_summary;

-- Direct Query
SELECT
    w.WarehouseID,
    w.Name AS WarehouseName,
    w.City,
    res.ResourceID,
    res.ResourceName,
    res.ResourceType,
    res.Unit,
    inv.QuantityAvailable,
    inv.QuantityDispatched,
    inv.QuantityConsumed,
    res.LowStockThreshold,
    CASE
        WHEN inv.QuantityAvailable < res.LowStockThreshold THEN 'LOW STOCK'
        ELSE 'OK'
    END AS StockStatus,
    inv.LastUpdated
FROM INVENTORY inv
JOIN WAREHOUSE w ON w.WarehouseID = inv.WarehouseID
JOIN RESOURCE res ON res.ResourceID = inv.ResourceID;

--View 3
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_financial_summary;

-- Direct Query
SELECT
    t.TransactionID,
    t.TransactionType,
    t.Amount,
    t.TransactionDate,
    t.Status,
    t.Description,
    d.Name AS DonorName,
    d.DonorType,
    s.SupplierName,
    e.EventName,
    res.ResourceName,
    u.FullName AS ApprovedByUser
FROM FINANCIALTRANSACTION t
LEFT JOIN DONOR d ON d.DonorID = t.DonorID
LEFT JOIN SUPPLIER s ON s.SupplierID = t.SupplierID
LEFT JOIN DISASTEREVENT e ON e.EventID = t.EventID
LEFT JOIN RESOURCE res ON res.ResourceID = t.ResourceID
LEFT JOIN [USER] u ON u.UserID = t.ApprovedBy;


--View 4
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_team_status;

-- Direct Query
SELECT
    rt.TeamID,
    rt.TeamName,
    rt.TeamType,
    rt.AvailabilityStatus,
    rt.Capacity,
    rt.ContactNumber,
    COUNT(tm.MemberID) AS TotalMembers,
    rt.Latitude,
    rt.Longitude
FROM RESCUETEAM rt
LEFT JOIN TEAMMEMBER tm ON tm.TeamID = rt.TeamID
GROUP BY
    rt.TeamID, rt.TeamName, rt.TeamType, rt.AvailabilityStatus,
    rt.Capacity, rt.ContactNumber, rt.Latitude, rt.Longitude;

--View 5
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_hospital_capacity;

-- Direct Query
SELECT
    h.HospitalID,
    h.Name AS HospitalName,
    h.City,
    h.TotalBeds,
    h.EmergencyCapacity,
    COUNT(pa.AdmissionID) AS CurrentAdmissions,
    h.TotalBeds - COUNT(pa.AdmissionID) AS BedsAvailable
FROM HOSPITAL h
LEFT JOIN PATIENTADMISSION pa
    ON pa.HospitalID = h.HospitalID
    AND pa.DischargeTime IS NULL
GROUP BY
    h.HospitalID, h.Name, h.City, h.TotalBeds, h.EmergencyCapacity;

--View 6
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_pending_approvals;

-- Direct Query
SELECT
    ar.RequestID,
    ar.RequestType,
    ar.RequestedAt,
    ar.Status,
    ar.Remarks,
    ru.FullName AS RequestedByUser,
    au.FullName AS ApprovedByUser
FROM APPROVALREQUEST ar
JOIN [USER] ru ON ru.UserID = ar.RequestedBy
LEFT JOIN [USER] au ON au.UserID = ar.ApprovedBy
WHERE ar.Status = 'Pending';

--View 7
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_event_budget_analysis;

-- Direct Query
SELECT
    e.EventID,
    e.EventName,
    e.DisasterType,
    e.Status,
    e.TotalBudget,
    ISNULL(SUM(CASE
        WHEN t.TransactionType = 'Donation' AND t.Status = 'Approved'
        THEN t.Amount ELSE 0 END), 0) AS TotalDonations,

    ISNULL(SUM(CASE
        WHEN t.TransactionType IN ('Expense','Procurement')
        AND t.Status = 'Approved'
        THEN t.Amount ELSE 0 END), 0) AS TotalExpenses,

    e.TotalBudget -
    ISNULL(SUM(CASE
        WHEN t.TransactionType IN ('Expense','Procurement')
        AND t.Status = 'Approved'
        THEN t.Amount ELSE 0 END), 0) AS RemainingBudget
FROM DISASTEREVENT e
LEFT JOIN FINANCIALTRANSACTION t ON t.EventID = e.EventID
GROUP BY
    e.EventID, e.EventName, e.DisasterType, e.Status, e.TotalBudget;
--View 8
SET STATISTICS TIME ON;

-- View
SELECT * FROM vw_audit_trail;

-- Direct Query
SELECT
    al.LogID,
    al.ActionType,
    al.TableAffected,
    al.OldValue,
    al.NewValue,
    al.LogTimestamp,
    u.FullName AS PerformedBy
FROM AUDITLOG al
LEFT JOIN [USER] u ON u.UserID = al.UserID;