package com.shopsense;

import com.shopsense.model.Customer;
import com.shopsense.model.Transaction;
import com.shopsense.model.CreditTransaction;
import com.shopsense.model.PaymentTransaction;
import com.shopsense.exception.CreditLimitExceededException;
import com.shopsense.util.StorageManager;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Verification test suite for testing the core OOP business logic,
 * balance math, custom exception handling, and file I/O operations.
 */
public class ShopSenseTest {
    public static void main(String[] args) {
        System.out.println("=== Starting ShopSense Backend Tests ===");
        int totalTests = 5;
        int passedTests = 0;

        try {
            // Test 1: Inheritance, polymorphism and balance calculation
            System.out.print("Test 1: Core Balance Math & Polymorphism... ");
            Customer customer1 = new Customer("C001", "Ryan Nasir", "03001234567", "ryan@domain.com", 5000.0);
            customer1.getAccount().addTransaction(1200.0, "Grocery Purchase", "Credit", LocalDate.now(), "Groceries");
            customer1.getAccount().addTransaction(800.0, "Medicine Purchase", "Credit", LocalDate.now(), "Medicines");
            customer1.getAccount().addTransaction(500.0, "Paid back partially", "Payment", LocalDate.now());

            double balance = customer1.getAccount().calculateBalance();
            // 1200 + 800 - 500 = 1500
            if (balance == 1500.0) {
                System.out.println("PASSED (Balance is exactly Rs. 1500.0)");
                passedTests++;
            } else {
                System.out.println("FAILED (Expected 1500.0, got " + balance + ")");
            }

            // Test 2: Credit Limit alert logic (checkCreditLimit)
            System.out.print("Test 2: Credit Limit Checking (Alertable)... ");
            if (!customer1.checkCreditLimit()) {
                // Balance is 1500, limit is 5000 -> Should not be exceeded
                customer1.getAccount().addTransaction(4000.0, "Large Electronics Purchase", "Credit", LocalDate.now(), "Household");
                // Balance is now 5500, limit is 5000 -> Should exceed
                if (customer1.checkCreditLimit()) {
                    System.out.println("PASSED (Correctly flagged limit breach)");
                    passedTests++;
                } else {
                    System.out.println("FAILED (Failed to flag limit breach at balance " + customer1.getAccount().calculateBalance() + ")");
                }
            } else {
                System.out.println("FAILED (Prematurely flagged limit breach at balance 1500)");
            }

            // Test 3: Custom Exception raising (sendAlert)
            System.out.print("Test 3: Custom Exception Raising... ");
            Customer customer2 = new Customer("C002", "Umar Wahaj", "03112223344", "umar@domain.com", 1000.0);
            customer2.getAccount().addTransaction(1500.0, "Bulk Milk Cartons", "Credit", LocalDate.now(), "Groceries");
            try {
                customer2.sendAlert();
                System.out.println("FAILED (Exception was not thrown on limit breach)");
            } catch (CreditLimitExceededException e) {
                System.out.println("PASSED (Successfully threw CreditLimitExceededException)");
                // System.out.println("Exception message: " + e.getMessage());
                passedTests++;
            }

            // Test 4: File Persistence (Save)
            System.out.print("Test 4: File I/O Serialization (Save)... ");
            // Clean up any old files first
            File cFile = new File("data/customers.txt");
            File tFile = new File("data/transactions.txt");
            if (cFile.exists()) cFile.delete();
            if (tFile.exists()) tFile.delete();

            ArrayList<Customer> saveList = new ArrayList<>();
            saveList.add(customer1);
            saveList.add(customer2);

            StorageManager.saveData(saveList);

            if (cFile.exists() && tFile.exists() && cFile.length() > 0 && tFile.length() > 0) {
                System.out.println("PASSED (Data files created successfully)");
                passedTests++;
            } else {
                System.out.println("FAILED (Persistence files are missing or empty)");
            }

            // Test 5: File Persistence (Load)
            System.out.print("Test 5: File I/O Deserialization (Load)... ");
            ArrayList<Customer> loadList = StorageManager.loadData();
            if (loadList.size() == 2) {
                Customer loadedC1 = loadList.get(0).getId().equals("C001") ? loadList.get(0) : loadList.get(1);
                double loadedBalance = loadedC1.getAccount().calculateBalance();
                if (loadedBalance == 5500.0) {
                    System.out.println("PASSED (Successfully reloaded 2 customers and verified exact balance Rs. 5500.0)");
                    passedTests++;
                } else {
                    System.out.println("FAILED (Expected balance 5500.0, got " + loadedBalance + ")");
                }
            } else {
                System.out.println("FAILED (Expected 2 customers, loaded " + loadList.size() + ")");
            }

        } catch (Exception e) {
            System.out.println("CRITICAL FAILURE in test suite: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Verification Results: " + passedTests + "/" + totalTests + " Tests Passed ===");
        if (passedTests == totalTests) {
            System.out.println("STATUS: ALL TESTS PASSED! Core classes are correct and ready for GUI integration.\n");
        } else {
            System.out.println("STATUS: TEST FAILURES ENCOUNTERED. Please investigate.\n");
        }
    }
}
