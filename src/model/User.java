package model;

import java.io.Serializable;
import java.util.Objects;

// Class representing a user in the store system, implementing Serializable to allow user objects to be serialized for storage or transmission.
public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Unique identifier for the class version, used during serialization to ensure the class version matches the serialized object

    // User fields
    private String employeeId;
    private String fullName;
    private String idNumber;
    private String phone;
    private String bankAccountNumber;
    private String branchId;
    private Role role;
    private String password;

    // Constructor for the User class that accepts all required fields for creating a new user object
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

    // Getters and setters for the user fields
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
        // Checking if the current object is the same object as the one passed, and if not, checking if it's of the same type and comparing the employee ID
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(employeeId, user.employeeId);
    }

    @Override
    public int hashCode() {
        // Returning the hash code based on the employee ID, used for storing the object in data structures like HashMap or HashSet
        return Objects.hash(employeeId);
    }

    @Override
    public String toString() {
        // Returning a string representation of the object, including the employee ID, full name, role, branch, and phone number
        return String.format("Employee #%s: %s | Role: %s | Branch: %s | Phone: %s",
                employeeId, fullName, role.getTitle(), branchId, phone);
    }
}