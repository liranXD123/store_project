package model;

import java.io.Serializable;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // מזהה ייחודי לגרסה של המחלקה, משמש בעת סריאליזציה כדי לוודא שהגרסה של המחלקה תואמת לגרסה של האובייקט המוסר

    // שדות המשתמש
    private String employeeId;
    private String fullName;
    private String idNumber;
    private String phone;
    private String bankAccountNumber;
    private String branchId;
    private Role role;
    private String password;

    // בנאי למחלקת User שמקבל את כל השדות הנדרשים ליצירת אובייקט משתמש חדש
    public User(String employeeId, String fullName, String idNumber, String phone, 
                String bankAccountNumber, String branchId, Role role, String password) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.bankAccountNumber = bankAccountNumber;
        this.branchId = branchId;
        this.role = role;
        this.password = password;
    }

    // גטרים וסטרים לשדות המשתמש
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeId() { return employeeId; }
    public String getFullName() { return fullName; }
    public String getIdNumber() { return idNumber; }
    public String getPhone() { return phone; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public String getBranchId() { return branchId; }
    public Role getRole() { return role; }
    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public boolean validatePassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }

    @Override
    public boolean equals(Object o) {
        // בדיקה אם האובייקט הנוכחי הוא אותו אובייקט כמו האובייקט המועבר, ואם לא, בדיקה אם הוא מאותו סוג והשוואת מזהה העובד
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(employeeId, user.employeeId);
    }

    @Override
    public int hashCode() {
        // החזרת קוד hash מבוסס על מזהה העובד, משמש לאחסון האובייקט במבני נתונים כמו HashMap או HashSet
        return Objects.hash(employeeId);
    }

    @Override
    public String toString() {
        // החזרת מחרוזת שמייצגת את האובייקט, כולל מזהה העובד, שם מלא, תפקיד, סניף ומספר טלפון
        return String.format("Employee #%s: %s | Role: %s | Branch: %s | Phone: %s",
                employeeId, fullName, role.getTitle(), branchId, phone);
    }
}