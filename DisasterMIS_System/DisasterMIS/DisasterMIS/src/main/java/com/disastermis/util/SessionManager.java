package com.disastermis.util;

public class SessionManager {

    private static SessionManager instance;

    private int    userId;
    private String fullName;
    private String email;
    private String roleName;
    private int    roleId;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int userId, String fullName, String email, int roleId, String roleName) {
        this.userId   = userId;
        this.fullName = fullName;
        this.email    = email;
        this.roleId   = roleId;
        this.roleName = roleName;
    }

    public void logout() {
        userId   = 0;
        fullName = null;
        email    = null;
        roleName = null;
        roleId   = 0;
    }

    public int    getUserId()   { return userId;   }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email;    }
    public String getRoleName() { return roleName; }
    public int    getRoleId()   { return roleId;   }

    public boolean isAdmin()             { return "Administrator".equalsIgnoreCase(roleName); }
    public boolean isEmergencyOperator() { return "Emergency Operator".equalsIgnoreCase(roleName); }
    public boolean isFieldOfficer()      { return "Field Officer".equalsIgnoreCase(roleName); }
    public boolean isWarehouseManager()  { return "Warehouse Manager".equalsIgnoreCase(roleName); }
    public boolean isFinanceOfficer()    { return "Finance Officer".equalsIgnoreCase(roleName); }
}
