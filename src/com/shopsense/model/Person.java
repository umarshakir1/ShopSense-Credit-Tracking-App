package com.shopsense.model;

/**
 * Base class representing a Person in the ShopSense system.
 * Encapsulates demographic information shared across different types of people.
 */
public class Person {
    private String id;
    private String name;
    private String phoneNumber;
    private String email;

    public Person(String id, String name, String phoneNumber, String email) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Person[ID=" + id + ", Name=" + name + ", Phone=" + phoneNumber + ", Email=" + email + "]";
    }
}
