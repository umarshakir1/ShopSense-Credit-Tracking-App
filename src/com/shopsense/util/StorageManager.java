package com.shopsense.util;

import com.shopsense.model.Customer;
import com.shopsense.model.Transaction;
import com.shopsense.model.CreditTransaction;
import com.shopsense.model.PaymentTransaction;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Storage manager that handles reading/writing customer and transaction data to text files.
 */
public class StorageManager {
    private static final String DATA_DIR = "data";
    private static final String CUSTOMERS_FILE = DATA_DIR + "/customers.txt";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "/transactions.txt";

    /**
     * Saves all customers and their complete transaction ledger to persistent text files.
     * @param customers list of customer records
     */
    public static synchronized void saveData(ArrayList<Customer> customers) {
        try {
            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save Customers
            BufferedWriter custWriter = new BufferedWriter(new FileWriter(CUSTOMERS_FILE));
            for (Customer c : customers) {
                custWriter.write(String.format("%s|%s|%s|%s|%.2f",
                        escapePipe(c.getId()),
                        escapePipe(c.getName()),
                        escapePipe(c.getPhoneNumber()),
                        escapePipe(c.getEmail()),
                        c.getCreditLimit()
                ));
                custWriter.newLine();
            }
            custWriter.close();

            // Save Transactions
            BufferedWriter txWriter = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE));
            for (Customer c : customers) {
                for (Transaction t : c.getAccount().getTransactions()) {
                    String category = (t instanceof CreditTransaction) ? ((CreditTransaction) t).getCategory() : "";
                    txWriter.write(String.format("%s|%s|%.2f|%s|%s|%s|%s",
                            escapePipe(t.getTransactionId()),
                            escapePipe(c.getId()), // Relational link to customer
                            t.getAmount(),
                            t.getDate().toString(),
                            escapePipe(t.getNotes()),
                            escapePipe(t.getTransactionType()),
                            escapePipe(category)
                    ));
                    txWriter.newLine();
                }
            }
            txWriter.close();
        } catch (IOException e) {
            System.err.println("Error saving ShopSense data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads all customer and transaction data from persistent text files.
     * Automatically links transactions to their respective customer accounts.
     * @return list of loaded customer records
     */
    public static synchronized ArrayList<Customer> loadData() {
        ArrayList<Customer> customers = new ArrayList<>();
        File custFile = new File(CUSTOMERS_FILE);
        File txFile = new File(TRANSACTIONS_FILE);

        // If customer file does not exist, return empty list
        if (!custFile.exists()) {
            return customers;
        }

        // Load Customers
        try (BufferedReader custReader = new BufferedReader(new FileReader(custFile))) {
            String line;
            while ((line = custReader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String id = unescapePipe(parts[0]);
                    String name = unescapePipe(parts[1]);
                    String phone = unescapePipe(parts[2]);
                    String email = unescapePipe(parts[3]);
                    double creditLimit = Double.parseDouble(parts[4]);

                    Customer customer = new Customer(id, name, phone, email, creditLimit);
                    customers.add(customer);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading customers: " + e.getMessage());
            e.printStackTrace();
        }

        // Load and link Transactions if file exists
        if (txFile.exists() && !customers.isEmpty()) {
            try (BufferedReader txReader = new BufferedReader(new FileReader(txFile))) {
                String line;
                while ((line = txReader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        String txId = unescapePipe(parts[0]);
                        String customerId = unescapePipe(parts[1]);
                        double amount = Double.parseDouble(parts[2]);
                        LocalDate date = LocalDate.parse(parts[3]);
                        String notes = unescapePipe(parts[4]);
                        String type = unescapePipe(parts[5]);
                        String category = parts.length > 6 ? unescapePipe(parts[6]) : "";

                        // Find customer using linear search
                        Customer associatedCustomer = null;
                        for (Customer c : customers) {
                            if (c.getId().equals(customerId)) {
                                associatedCustomer = c;
                                break;
                            }
                        }

                        if (associatedCustomer != null) {
                            Transaction transaction;
                            if ("Payment".equalsIgnoreCase(type)) {
                                transaction = new PaymentTransaction(txId, amount, date, notes);
                            } else {
                                transaction = new CreditTransaction(txId, amount, date, notes, category.isEmpty() ? "Others" : category);
                            }
                            associatedCustomer.getAccount().addTransaction(transaction);
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error loading transactions: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return customers;
    }

    // Helper utility to make text pipe-safe
    private static String escapePipe(String str) {
        if (str == null) return "";
        return str.replace("|", "\\pipe");
    }

    private static String unescapePipe(String str) {
        if (str == null) return "";
        return str.replace("\\pipe", "|");
    }
}
