package com.shopsense;

import com.shopsense.model.*;
import com.shopsense.exception.CreditLimitExceededException;
import com.shopsense.exception.CustomerNotFoundException;
import com.shopsense.util.StorageManager;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main JavaFX GUI application for ShopSense.
 * Implements a modern dark glassmorphism dashboard containing sidebar navigation,
 * dynamic metric summaries, complete customer CRUD, transaction ledger with
 * limit override dialogs, printable receipt export, and beautiful analytics charts.
 */
public class ShopSenseApp extends Application {

    private ArrayList<Customer> customerList = new ArrayList<>();
    private ObservableList<Customer> observableCustomerList = FXCollections.observableArrayList();
    private Customer selectedCustomerForLedger = null;

    // Metric Labels
    private Label lblTotalOutstanding = new Label("Rs. 0.00");
    private Label lblTotalCredit = new Label("Rs. 0.00");
    private Label lblTotalPayments = new Label("Rs. 0.00");
    private Label lblRiskAccounts = new Label("0");

    // UI Navigation Panels
    private StackPane contentArea = new StackPane();
    private VBox dashboardView;
    private VBox customersView;
    private VBox ledgerView;
    private VBox reportsView;

    // Navigation buttons
    private Button btnNavDashboard;
    private Button btnNavCustomers;
    private Button btnNavLedger;
    private Button btnNavReports;

    @Override
    public void start(Stage primaryStage) {
        // Load data from text file persistence
        customerList = StorageManager.loadData();
        observableCustomerList.addAll(customerList);

        // Sidebar Navigation
        VBox sidebar = createSidebar(primaryStage);

        // Content Views
        createAllViews();

        // Main Layout
        HBox mainLayout = new HBox();
        mainLayout.getChildren().addAll(sidebar, contentArea);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Calculate and load initially
        refreshMetrics();
        showView(dashboardView, btnNavDashboard);

        // Scene
        Scene scene = new Scene(mainLayout, 1150, 750);
        
        // Load External Stylesheet
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }

        primaryStage.setTitle("ShopSense - Smart Credit Tracking & Retail Billing");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshMetrics() {
        double totalOutstanding = 0;
        double totalCredit = 0;
        double totalPayments = 0;
        int riskCount = 0;

        for (Customer c : customerList) {
            double bal = c.getAccount().calculateBalance();
            totalOutstanding += bal;

            if (c.checkCreditLimit()) {
                riskCount++;
            }

            for (Transaction t : c.getAccount().getTransactions()) {
                if (t instanceof CreditTransaction) {
                    totalCredit += t.getAmount();
                } else if (t instanceof PaymentTransaction) {
                    totalPayments += t.getAmount();
                }
            }
        }

        lblTotalOutstanding.setText(String.format("Rs. %.2f", totalOutstanding));
        lblTotalCredit.setText(String.format("Rs. %.2f", totalCredit));
        lblTotalPayments.setText(String.format("Rs. %.2f", totalPayments));
        lblRiskAccounts.setText(String.valueOf(riskCount));
    }

    private void showView(VBox targetView, Button activeBtn) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(targetView);

        // Reset nav styles
        btnNavDashboard.getStyleClass().remove("nav-btn-active");
        btnNavCustomers.getStyleClass().remove("nav-btn-active");
        btnNavLedger.getStyleClass().remove("nav-btn-active");
        btnNavReports.getStyleClass().remove("nav-btn-active");

        activeBtn.getStyleClass().add("nav-btn-active");
    }

    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(250);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setSpacing(10);
        sidebar.getStyleClass().add("sidebar");

        // App Logo/Brand
        HBox brandBox = new HBox();
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setSpacing(10);
        brandBox.setPadding(new Insets(10, 0, 30, 0));

        Text brandIcon = new Text("💰");
        brandIcon.setFont(Font.font("Segoe UI", 24));
        Text brandName = new Text("ShopSense");
        brandName.setFont(Font.font("Poppins", FontWeight.BOLD, 22));
        brandName.setFill(Color.web("#1E2A14"));
        brandBox.getChildren().addAll(brandIcon, brandName);

        // Nav buttons
        btnNavDashboard = new Button("📊  Dashboard");
        btnNavDashboard.getStyleClass().add("nav-btn");
        btnNavDashboard.setMaxWidth(Double.MAX_VALUE);
        btnNavDashboard.setOnAction(e -> {
            refreshMetrics();
            setupDashboardView(); // Reload high-risk list
            showView(dashboardView, btnNavDashboard);
        });

        btnNavCustomers = new Button("👥  Customer List");
        btnNavCustomers.getStyleClass().add("nav-btn");
        btnNavCustomers.setMaxWidth(Double.MAX_VALUE);
        btnNavCustomers.setOnAction(e -> {
            observableCustomerList.setAll(customerList);
            showView(customersView, btnNavCustomers);
        });

        btnNavLedger = new Button("📓  Udhaar Ledger");
        btnNavLedger.getStyleClass().add("nav-btn");
        btnNavLedger.setMaxWidth(Double.MAX_VALUE);
        btnNavLedger.setOnAction(e -> {
            showView(ledgerView, btnNavLedger);
        });

        btnNavReports = new Button("📈  Analytics & Reports");
        btnNavReports.getStyleClass().add("nav-btn");
        btnNavReports.setMaxWidth(Double.MAX_VALUE);
        btnNavReports.setOnAction(e -> {
            setupReportsView();
            showView(reportsView, btnNavReports);
        });

        // Developer Info footer
        VBox footerBox = new VBox();
        footerBox.setSpacing(5);
        footerBox.setPadding(new Insets(10));
        footerBox.setStyle("-fx-background-color: #EDF3E6; -fx-background-radius: 8px; -fx-border-color: #C8D9B5; -fx-border-radius: 8px; -fx-border-width: 1px;");
        
        Label lblLab = new Label("OOP Open-Ended Lab");
        lblLab.setStyle("-fx-text-fill: #7AB342; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label lblMembers = new Label("Group Members:");
        lblMembers.setStyle("-fx-text-fill: #5A6650; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label lblM1 = new Label("Ryan Nasir (74832)");
        lblM1.setStyle("-fx-text-fill: #1E2A14; -fx-font-family: 'Inter'; -fx-font-size: 10px;");
        Label lblM2 = new Label("Muhammad Umar (74786)");
        lblM2.setStyle("-fx-text-fill: #1E2A14; -fx-font-family: 'Inter'; -fx-font-size: 10px;");
        Label lblM3 = new Label("Muhammad Wahaj (74742)");
        lblM3.setStyle("-fx-text-fill: #1E2A14; -fx-font-family: 'Inter'; -fx-font-size: 10px;");
        Label lblM4 = new Label("M. Asim Abbasi (74844)");
        lblM4.setStyle("-fx-text-fill: #1E2A14; -fx-font-family: 'Inter'; -fx-font-size: 10px;");
        
        footerBox.getChildren().addAll(lblLab, lblMembers, lblM1, lblM2, lblM3, lblM4);

        Button btnNavExit = new Button("❌  Exit Program");
        btnNavExit.getStyleClass().add("nav-btn-exit");
        btnNavExit.setMaxWidth(Double.MAX_VALUE);
        btnNavExit.setOnAction(e -> confirmAndExit(stage));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(brandBox, btnNavDashboard, btnNavCustomers, btnNavLedger, btnNavReports, btnNavExit, spacer, footerBox);
        return sidebar;
    }

    private void createAllViews() {
        dashboardView = new VBox();
        dashboardView.setPadding(new Insets(30));
        dashboardView.setSpacing(25);
        setupDashboardView();

        customersView = new VBox();
        customersView.setPadding(new Insets(30));
        customersView.setSpacing(20);
        setupCustomersView();

        ledgerView = new VBox();
        ledgerView.setPadding(new Insets(30));
        ledgerView.setSpacing(20);
        setupLedgerView();

        reportsView = new VBox();
        reportsView.setPadding(new Insets(30));
        reportsView.setSpacing(20);
        setupReportsView();
    }

    // ==========================================
    // MODULE 1: DASHBOARD VIEW
    // ==========================================
    private void setupDashboardView() {
        dashboardView.getChildren().clear();

        // Header
        VBox headerBox = new VBox();
        Label title = new Label("Smart Credit Dashboard");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Welcome back to ShopSense. Real-time register overview:");
        subtitle.getStyleClass().add("section-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Metrics Grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(20);

        // Card 1: Total Outstanding
        VBox cardOut = createMetricCard("TOTAL OUTSTANDING BALANCE (UDHAAR)", lblTotalOutstanding, "metric-outstanding");
        metricsGrid.add(cardOut, 0, 0);
        GridPane.setHgrow(cardOut, Priority.ALWAYS);

        // Card 2: Total Credit
        VBox cardCred = createMetricCard("TOTAL CREDIT EXTENDED", lblTotalCredit, "metric-credit");
        metricsGrid.add(cardCred, 1, 0);
        GridPane.setHgrow(cardCred, Priority.ALWAYS);

        // Card 3: Total Payments
        VBox cardPay = createMetricCard("TOTAL PAYMENTS RECEIVED", lblTotalPayments, "metric-payments");
        metricsGrid.add(cardPay, 2, 0);
        GridPane.setHgrow(cardPay, Priority.ALWAYS);

        // Card 4: Risk Accounts
        VBox cardRisk = createMetricCard("CREDIT LIMIT BREACH ALERTS", lblRiskAccounts, "metric-risk");
        metricsGrid.add(cardRisk, 3, 0);
        GridPane.setHgrow(cardRisk, Priority.ALWAYS);

        // Alerts and Top Risk Customers Table
        VBox riskAlertsBox = new VBox();
        riskAlertsBox.setSpacing(15);
        riskAlertsBox.getStyleClass().add("glass-panel");
        VBox.setVgrow(riskAlertsBox, Priority.ALWAYS);

        Label alertsTitle = new Label("⚠ ACTIVE CREDIT LIMIT BREACHES (HIGH RISK)");
        alertsTitle.setStyle("-fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #D94F4F;");
        
        TableView<Customer> riskTable = new TableView<>();
        riskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Customer, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(80);

        TableColumn<Customer, String> colName = new TableColumn<>("Customer Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<Customer, String> colPhone = new TableColumn<>("Phone Number");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colPhone.setPrefWidth(150);

        TableColumn<Customer, Double> colBal = new TableColumn<>("Outstanding Balance");
        colBal.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAccount().calculateBalance()).asObject());
        colBal.setCellFactory(column -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                    setTextFill(Color.web("#D94F4F"));
                    setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Customer, Double> colLimit = new TableColumn<>("Allowed Credit Limit");
        colLimit.setCellValueFactory(new PropertyValueFactory<>("creditLimit"));
        colLimit.setCellFactory(column -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                }
            }
        });

        TableColumn<Customer, Void> colAction = new TableColumn<>("Actions");
        colAction.setCellFactory(column -> new TableCell<Customer, Void>() {
            private final Button btnResolve = new Button("Add Repayment");
            {
                btnResolve.getStyleClass().add("btn-success");
                btnResolve.setStyle("-fx-padding: 4px 10px; -fx-font-size: 11px;");
                btnResolve.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    selectedCustomerForLedger = customer;
                    // Go to ledger
                    showView(ledgerView, btnNavLedger);
                    setupLedgerView();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnResolve);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        riskTable.getColumns().addAll(colId, colName, colPhone, colBal, colLimit, colAction);

        // Load breached accounts
        ArrayList<Customer> riskCustomers = customerList.stream()
                .filter(Customer::checkCreditLimit)
                .collect(Collectors.toCollection(ArrayList::new));
        riskTable.setItems(FXCollections.observableArrayList(riskCustomers));
        riskTable.setPlaceholder(new Label("Great job! No customer has exceeded their credit limit right now."));

        riskAlertsBox.getChildren().addAll(alertsTitle, riskTable);

        dashboardView.getChildren().addAll(headerBox, metricsGrid, riskAlertsBox);
    }

    private VBox createMetricCard(String titleStr, Label valueLabel, String styleClass) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.getStyleClass().addAll("metric-card", styleClass);

        Label title = new Label(titleStr);
        title.getStyleClass().add("metric-title");
        title.setWrapText(true);

        valueLabel.getStyleClass().add("metric-value");

        // Hover Micro-animations (Scaling)
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.03);
            card.setScaleY(1.03);
            card.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(99,102,241,0.2), 15, 0, 0, 0);");
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            card.setStyle(null);
        });

        card.getChildren().addAll(title, valueLabel);
        return card;
    }

    // ==========================================
    // MODULE 2: CUSTOMERS CRUD VIEW
    // ==========================================
    private void setupCustomersView() {
        customersView.getChildren().clear();

        // Header and search bar
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        
        VBox headerBox = new VBox();
        Label title = new Label("Customer Registry Management");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Add, edit, search and delete customer accounts (Full CRUD)");
        subtitle.getStyleClass().add("section-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAddCustomer = new Button("➕ Add New Customer");
        btnAddCustomer.getStyleClass().add("btn-primary");
        btnAddCustomer.setOnAction(e -> openCustomerFormDialog(null));

        topBar.getChildren().addAll(headerBox, spacer, btnAddCustomer);

        // Search Bar (Linear search using loops)
        HBox searchBarBox = new HBox();
        searchBarBox.setAlignment(Pos.CENTER_LEFT);
        searchBarBox.setSpacing(10);
        searchBarBox.setPadding(new Insets(10, 0, 10, 0));

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Search by customer name, ID or phone number...");
        txtSearch.getStyleClass().add("text-input");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        // Real-time linear search algorithm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            performLinearSearch(newValue);
        });

        Button btnClear = new Button("Clear");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            observableCustomerList.setAll(customerList);
        });

        searchBarBox.getChildren().addAll(txtSearch, btnClear);

        // Customer table
        TableView<Customer> tblCustomers = new TableView<>();
        tblCustomers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Customer, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<Customer, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(160);

        TableColumn<Customer, String> colPhone = new TableColumn<>("Phone Number");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colPhone.setPrefWidth(120);

        TableColumn<Customer, String> colEmail = new TableColumn<>("Email Address");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(150);

        TableColumn<Customer, Double> colBal = new TableColumn<>("Outstanding");
        colBal.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAccount().calculateBalance()).asObject());
        colBal.setCellFactory(col -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                    setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
                    if (value > 0) {
                        setTextFill(Color.web("#FF8C42")); // Outstanding balance in ACCENT
                    } else {
                        setTextFill(Color.web("#7AB342")); // Paid up in SUCCESS
                    }
                }
            }
        });

        TableColumn<Customer, Double> colLimit = new TableColumn<>("Credit Limit");
        colLimit.setCellValueFactory(new PropertyValueFactory<>("creditLimit"));
        colLimit.setCellFactory(col -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                    setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: #1E2A14;");
                }
            }
        });

        TableColumn<Customer, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> {
            boolean overLimit = cell.getValue().checkCreditLimit();
            return new SimpleStringProperty(overLimit ? "⚠ BREACH" : "✅ NORMAL");
        });
        colStatus.setCellFactory(col -> new TableCell<Customer, String>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(val);
                    if (val.contains("BREACH")) {
                        badge.getStyleClass().add("badge-danger");
                    } else {
                        badge.getStyleClass().add("badge-normal");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Customer, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<Customer, Void>() {
            private final Button btnEdit = new Button("📝");
            private final Button btnDelete = new Button("🗑️");
            private final Button btnViewLedger = new Button("📓");
            private final HBox pane = new HBox(btnViewLedger, btnEdit, btnDelete);

            {
                pane.setSpacing(8);
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-secondary");
                btnEdit.setStyle("-fx-padding: 6px 10px; -fx-font-size: 11px;");
                btnEdit.setTooltip(new Tooltip("Edit Customer"));
                btnEdit.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    openCustomerFormDialog(customer);
                });

                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 6px 10px; -fx-font-size: 11px;");
                btnDelete.setTooltip(new Tooltip("Delete Customer"));
                btnDelete.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    confirmAndDeleteCustomer(customer);
                });

                btnViewLedger.getStyleClass().add("btn-success");
                btnViewLedger.setStyle("-fx-padding: 6px 10px; -fx-font-size: 11px;");
                btnViewLedger.setTooltip(new Tooltip("View Ledger & Add Transactions"));
                btnViewLedger.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    selectedCustomerForLedger = customer;
                    showView(ledgerView, btnNavLedger);
                    setupLedgerView();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
        colActions.setPrefWidth(140);

        tblCustomers.getColumns().addAll(colId, colName, colPhone, colEmail, colBal, colLimit, colStatus, colActions);
        tblCustomers.setItems(observableCustomerList);
        VBox.setVgrow(tblCustomers, Priority.ALWAYS);

        customersView.getChildren().addAll(topBar, searchBarBox, tblCustomers);
    }

    // Linear Search implementation using loops and string inclusion checks
    private void performLinearSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            observableCustomerList.setAll(customerList);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        ArrayList<Customer> filtered = new ArrayList<>();
        
        // Loop implementation for linear search (Explicit requirement)
        for (int i = 0; i < customerList.size(); i++) {
            Customer c = customerList.get(i);
            if (c.getId().toLowerCase().contains(lowerQuery) ||
                c.getName().toLowerCase().contains(lowerQuery) ||
                c.getPhoneNumber().toLowerCase().contains(lowerQuery) ||
                c.getEmail().toLowerCase().contains(lowerQuery)) {
                filtered.add(c);
            }
        }
        observableCustomerList.setAll(filtered);
    }

    private void openCustomerFormDialog(Customer targetCustomer) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(targetCustomer == null ? "Add New Customer" : "Edit Customer Details");

        VBox form = new VBox();
        form.setPadding(new Insets(25));
        form.setSpacing(15);
        form.getStyleClass().add("dialog-pane");

        Label title = new Label(targetCustomer == null ? "🆕 Create Customer Account" : "📝 Edit Customer Profile");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #064e3b;");

        // Input Fields
        TextField txtId = new TextField();
        txtId.setPromptText("Enter Customer ID (e.g. C005)");
        txtId.getStyleClass().add("text-input");
        if (targetCustomer != null) {
            txtId.setText(targetCustomer.getId());
            txtId.setDisable(true); // Don't allow changing ID during edits
        }

        TextField txtName = new TextField();
        txtName.setPromptText("Enter Full Name");
        txtName.getStyleClass().add("text-input");
        if (targetCustomer != null) txtName.setText(targetCustomer.getName());

        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Enter Mobile Number (e.g. 03001234567)");
        txtPhone.getStyleClass().add("text-input");
        if (targetCustomer != null) txtPhone.setText(targetCustomer.getPhoneNumber());

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Enter Email Address");
        txtEmail.getStyleClass().add("text-input");
        if (targetCustomer != null) txtEmail.setText(targetCustomer.getEmail());

        TextField txtLimit = new TextField();
        txtLimit.setPromptText("Set Credit Limit (Rs.)");
        txtLimit.getStyleClass().add("text-input");
        if (targetCustomer != null) txtLimit.setText(String.valueOf(targetCustomer.getCreditLimit()));

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        // Form Buttons
        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setSpacing(10);

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(targetCustomer == null ? "Add Customer" : "Update Profile");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> {
            // Validations
            if (txtId.getText().trim().isEmpty() || txtName.getText().trim().isEmpty() || 
                txtPhone.getText().trim().isEmpty() || txtLimit.getText().trim().isEmpty()) {
                lblError.setText("Please fill out all required fields (*).");
                return;
            }

            // ID duplication check on new customers
            if (targetCustomer == null) {
                boolean duplicate = false;
                for (Customer c : customerList) {
                    if (c.getId().equalsIgnoreCase(txtId.getText().trim())) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) {
                    lblError.setText("Error: A customer with ID '" + txtId.getText().trim() + "' already exists!");
                    return;
                }
            }

            double limit;
            try {
                limit = Double.parseDouble(txtLimit.getText().trim());
                if (limit < 0) {
                    lblError.setText("Credit Limit cannot be negative.");
                    return;
                }
            } catch (NumberFormatException nfe) {
                lblError.setText("Credit Limit must be a valid number.");
                return;
            }

            if (targetCustomer == null) {
                // CREATE Operation
                Customer newCust = new Customer(
                    txtId.getText().trim(),
                    txtName.getText().trim(),
                    txtPhone.getText().trim(),
                    txtEmail.getText().trim(),
                    limit
                );
                customerList.add(newCust);
            } else {
                // UPDATE Operation
                targetCustomer.setName(txtName.getText().trim());
                targetCustomer.setPhoneNumber(txtPhone.getText().trim());
                targetCustomer.setEmail(txtEmail.getText().trim());
                targetCustomer.setCreditLimit(limit);
            }

            // Persist Changes
            StorageManager.saveData(customerList);
            observableCustomerList.setAll(customerList);
            refreshMetrics();
            dialog.close();
        });

        btnBox.getChildren().addAll(btnCancel, btnSave);

        // Assemble Form
        form.getChildren().addAll(
            title, 
            new Label("Customer ID *"), txtId,
            new Label("Full Name *"), txtName,
            new Label("Phone Number *"), txtPhone,
            new Label("Email"), txtEmail,
            new Label("Allowed Credit Limit (Rs.) *"), txtLimit,
            lblError,
            btnBox
        );

        Scene scene = new Scene(form, 400, 560);
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void confirmAndDeleteCustomer(Customer customer) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirm Deletion");

        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(15);
        root.getStyleClass().add("dialog-pane");

        Label title = new Label("⚠ Delete Customer Profile?");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #ef4444;");

        Label desc = new Label(String.format(
            "Are you sure you want to permanently delete customer '%s' (ID: %s)?\nThis will erase their complete transaction register history and outstanding balance of Rs. %.2f.",
            customer.getName(), customer.getId(), customer.getAccount().calculateBalance()
        ));
        desc.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setSpacing(10);

        Button btnCancel = new Button("Keep Profile");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnDelete = new Button("Delete Account");
        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setOnAction(e -> {
            // DELETE Operation
            customerList.remove(customer);
            StorageManager.saveData(customerList);
            observableCustomerList.setAll(customerList);
            
            // Unselect if it was selected in ledger
            if (selectedCustomerForLedger == customer) {
                selectedCustomerForLedger = null;
            }

            refreshMetrics();
            dialog.close();
        });

        btnBox.getChildren().addAll(btnCancel, btnDelete);
        root.getChildren().addAll(title, desc, btnBox);

        Scene scene = new Scene(root, 480, 180);
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==========================================
    // MODULE 3: TRANSACTION LEDGER VIEW
    // ==========================================
    private ComboBox<Customer> comboCustomerSelector;
    private TableView<Transaction> tblLedgerTransactions;
    private Label lblLedgerCustName = new Label("-");
    private Label lblLedgerCustId = new Label("-");
    private Label lblLedgerCustBal = new Label("Rs. 0.00");
    private Label lblLedgerCustLimit = new Label("Rs. 0.00");
    private Label lblLedgerCustStatus = new Label("-");
    private Button btnExportCompleteLedger;

    private void setupLedgerView() {
        ledgerView.getChildren().clear();

        // Header
        VBox headerBox = new VBox();
        Label title = new Label("Retail Udhaar Ledger & Billing");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Log credit purchases, record customer payments, export transaction invoices:");
        subtitle.getStyleClass().add("section-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Customer selection row
        HBox selectionBar = new HBox();
        selectionBar.setAlignment(Pos.CENTER_LEFT);
        selectionBar.setSpacing(15);
        selectionBar.getStyleClass().add("glass-panel");

        Label selectLbl = new Label("Select Customer Account:");
        selectLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");

        comboCustomerSelector = new ComboBox<>();
        comboCustomerSelector.setPromptText("Choose customer...");
        comboCustomerSelector.setPrefWidth(250);
        comboCustomerSelector.getStyleClass().add("combo-box");
        
        // Custom cell rendering to show Name + ID in the dropdown
        comboCustomerSelector.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getId() + ")");
                }
            }
        });
        comboCustomerSelector.setButtonCell(new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getId() + ")");
                }
            }
        });

        // Set items
        comboCustomerSelector.setItems(FXCollections.observableArrayList(customerList));

        // Listen for selections
        comboCustomerSelector.setOnAction(e -> {
            selectedCustomerForLedger = comboCustomerSelector.getSelectionModel().getSelectedItem();
            loadLedgerForSelectedCustomer();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        btnExportCompleteLedger = new Button("📄 Export Complete Ledger");
        btnExportCompleteLedger.getStyleClass().add("btn-primary");
        btnExportCompleteLedger.setDisable(true);
        btnExportCompleteLedger.setOnAction(e -> {
            if (selectedCustomerForLedger != null) {
                generateAndExportCompleteLedger(selectedCustomerForLedger);
            }
        });

        selectionBar.getChildren().addAll(selectLbl, comboCustomerSelector, spacer, btnExportCompleteLedger);

        // Account Profile Card and Transaction Entry Panel
        HBox middleLayout = new HBox();
        middleLayout.setSpacing(20);

        // Left Panel: Customer Summary
        VBox profileCard = new VBox();
        profileCard.setSpacing(15);
        profileCard.setPrefWidth(400);
        profileCard.getStyleClass().add("glass-panel");

        Label profileTitle = new Label("👥 CUSTOMER ACCOUNT PROFILE");
        profileTitle.setStyle("-fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-text-fill: #7AB342; -fx-font-size: 14px;");

        GridPane profileGrid = new GridPane();
        profileGrid.setHgap(15);
        profileGrid.setVgap(12);

        profileGrid.add(new Label("Customer Name:"), 0, 0);
        lblLedgerCustName.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-text-fill: #1E2A14;");
        profileGrid.add(lblLedgerCustName, 1, 0);

        profileGrid.add(new Label("Customer ID:"), 0, 1);
        lblLedgerCustId.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: #1E2A14;");
        profileGrid.add(lblLedgerCustId, 1, 1);

        profileGrid.add(new Label("Outstanding Bal:"), 0, 2);
        lblLedgerCustBal.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-size: 15px;");
        profileGrid.add(lblLedgerCustBal, 1, 2);

        profileGrid.add(new Label("Credit Limit:"), 0, 3);
        lblLedgerCustLimit.setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: #1E2A14;");
        profileGrid.add(lblLedgerCustLimit, 1, 3);

        profileGrid.add(new Label("Account Status:"), 0, 4);
        profileGrid.add(lblLedgerCustStatus, 1, 4);

        profileCard.getChildren().addAll(profileTitle, profileGrid);

        // Right Panel: Form Actions (Udhaar / Pay)
        VBox actionCard = new VBox();
        actionCard.setSpacing(15);
        actionCard.getStyleClass().add("glass-panel");
        HBox.setHgrow(actionCard, Priority.ALWAYS);

        Label actionTitle = new Label("💼 RECORD RETAIL TRANSACTION");
        actionTitle.setStyle("-fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-text-fill: #7AB342; -fx-font-size: 14px;");

        VBox formRow = new VBox();
        formRow.setSpacing(10);

        Button btnAddCredit = new Button("🛒 Log Credit (Udhaar) Purchase");
        btnAddCredit.getStyleClass().add("btn-primary");
        btnAddCredit.setMaxWidth(Double.MAX_VALUE);
        btnAddCredit.setOnAction(e -> openTransactionDialog(true));

        Button btnAddPayment = new Button("💵 Record Cash Payment");
        btnAddPayment.getStyleClass().add("btn-success");
        btnAddPayment.setMaxWidth(Double.MAX_VALUE);
        btnAddPayment.setOnAction(e -> openTransactionDialog(false));

        formRow.getChildren().addAll(btnAddCredit, btnAddPayment);

        actionCard.getChildren().addAll(actionTitle, new Label("Select transaction action to log against selected profile:"), formRow);

        middleLayout.getChildren().addAll(profileCard, actionCard);

        // Bottom Panel: Transaction History Ledger Table
        VBox ledgerTableBox = new VBox();
        ledgerTableBox.setSpacing(10);
        ledgerTableBox.getStyleClass().add("glass-panel");
        VBox.setVgrow(ledgerTableBox, Priority.ALWAYS);

        Label historyTitle = new Label("📓 TRANSACTION HISTORY LEDGER REGISTER");
        historyTitle.setStyle("-fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-size: 14px;");

        tblLedgerTransactions = new TableView<>();
        tblLedgerTransactions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> colTxId = new TableColumn<>("Tx ID");
        colTxId.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        colTxId.setPrefWidth(90);

        TableColumn<Transaction, String> colType = new TableColumn<>("Tx Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
        colType.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(val);
                    setStyle("-fx-font-family: 'Inter'; -fx-font-weight: bold;");
                    if ("Credit".equalsIgnoreCase(val)) {
                        setTextFill(Color.web("#FF8C42")); // ACCENT for credit
                    } else {
                        setTextFill(Color.web("#7AB342")); // SUCCESS for payment
                    }
                }
            }
        });

        TableColumn<Transaction, Double> colAmt = new TableColumn<>("Amount");
        colAmt.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmt.setCellFactory(col -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", value));
                    setStyle("-fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-text-fill: #1E2A14;");
                }
            }
        });

        TableColumn<Transaction, String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(cell -> {
            Transaction t = cell.getValue();
            String cat = (t instanceof CreditTransaction) ? ((CreditTransaction) t).getCategory() : "-";
            return new SimpleStringProperty(cat);
        });

        TableColumn<Transaction, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate().toString()));

        TableColumn<Transaction, String> colNotes = new TableColumn<>("Purchase Notes");
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        TableColumn<Transaction, Void> colPrint = new TableColumn<>("Invoices");
        colPrint.setCellFactory(col -> new TableCell<Transaction, Void>() {
            private final Button btnPrint = new Button("🖨️ Export Invoice");
            {
                btnPrint.getStyleClass().add("btn-secondary");
                btnPrint.setStyle("-fx-padding: 4px 8px; -fx-font-size: 11px;");
                btnPrint.setOnAction(event -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    generateAndExportReceipt(t);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnPrint);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        colPrint.setPrefWidth(140);

        tblLedgerTransactions.getColumns().addAll(colTxId, colType, colAmt, colCategory, colDate, colNotes, colPrint);
        tblLedgerTransactions.setPlaceholder(new Label("Select a customer above to view transaction logs."));
        VBox.setVgrow(tblLedgerTransactions, Priority.ALWAYS);

        ledgerTableBox.getChildren().addAll(historyTitle, tblLedgerTransactions);

        // Assemble view
        ledgerView.getChildren().addAll(headerBox, selectionBar, middleLayout, ledgerTableBox);

        // Pre-select if someone was passed in
        if (selectedCustomerForLedger != null) {
            comboCustomerSelector.setValue(selectedCustomerForLedger);
            loadLedgerForSelectedCustomer();
        }
    }

    private void loadLedgerForSelectedCustomer() {
        if (selectedCustomerForLedger == null) {
            lblLedgerCustName.setText("-");
            lblLedgerCustId.setText("-");
            lblLedgerCustBal.setText("Rs. 0.00");
            lblLedgerCustLimit.setText("Rs. 0.00");
            lblLedgerCustStatus.setText("-");
            tblLedgerTransactions.setItems(FXCollections.observableArrayList());
            if (btnExportCompleteLedger != null) {
                btnExportCompleteLedger.setDisable(true);
            }
            return;
        }

        if (btnExportCompleteLedger != null) {
            btnExportCompleteLedger.setDisable(false);
        }

        Customer c = selectedCustomerForLedger;
        double bal = c.getAccount().calculateBalance();
        lblLedgerCustName.setText(c.getName());
        lblLedgerCustId.setText(c.getId());
        lblLedgerCustBal.setText(String.format("Rs. %.2f", bal));
        lblLedgerCustLimit.setText(String.format("Rs. %.2f", c.getCreditLimit()));

        if (bal > 0) {
            lblLedgerCustBal.setTextFill(Color.web("#FF8C42")); // ACCENT
        } else {
            lblLedgerCustBal.setTextFill(Color.web("#7AB342")); // PRIMARY/SUCCESS
        }

        if (c.checkCreditLimit()) {
            lblLedgerCustStatus.setText("⚠ EXCEEDED LIMIT");
            lblLedgerCustStatus.setStyle("-fx-text-fill: #D94F4F; -fx-font-family: 'Inter'; -fx-font-weight: bold;");
        } else {
            lblLedgerCustStatus.setText("✅ BALANCE HEALTHY");
            lblLedgerCustStatus.setStyle("-fx-text-fill: #7AB342; -fx-font-family: 'Inter'; -fx-font-weight: bold;");
        }

        // Sort transactions chronologically (Newest first)
        ArrayList<Transaction> sortedTxs = new ArrayList<>(c.getAccount().getTransactions());
        sortedTxs.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

        tblLedgerTransactions.setItems(FXCollections.observableArrayList(sortedTxs));
    }

    private void openTransactionDialog(boolean isCredit) {
        if (selectedCustomerForLedger == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Customer Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a customer from the dropdown list first before logging transactions.");
            alert.showAndWait();
            return;
        }

        Customer targetCustomer = selectedCustomerForLedger;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isCredit ? "Record Credit Purchase (Udhaar)" : "Record Cash Repayment");

        VBox form = new VBox();
        form.setPadding(new Insets(25));
        form.setSpacing(15);
        form.getStyleClass().add("dialog-pane");

        Label title = new Label(isCredit ? "🛒 Log New Credit Purchase" : "💵 Record Repayment Payment");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #064e3b;");

        // Fields
        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Enter Amount in Rs. (e.g. 1500)");
        txtAmount.getStyleClass().add("text-input");

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.getStyleClass().add("date-picker");
        dpDate.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> comboCategory = new ComboBox<>();
        comboCategory.getStyleClass().add("combo-box");
        comboCategory.getItems().addAll("Groceries", "Medicines", "Household", "Others");
        comboCategory.setValue("Groceries");
        comboCategory.setMaxWidth(Double.MAX_VALUE);

        TextField txtNotes = new TextField();
        txtNotes.setPromptText("Enter additional notes/items purchased...");
        txtNotes.getStyleClass().add("text-input");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setSpacing(10);

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("Log Transaction");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> {
            String amtStr = txtAmount.getText().trim();
            if (amtStr.isEmpty()) {
                lblError.setText("Amount is a required field.");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    lblError.setText("Transaction amount must be greater than zero.");
                    return;
                }
            } catch (NumberFormatException nfe) {
                lblError.setText("Amount must be a valid number.");
                return;
            }

            LocalDate date = dpDate.getValue();
            if (date == null) {
                lblError.setText("Please select a transaction date.");
                return;
            }

            String notes = txtNotes.getText().trim();
            String cat = comboCategory.getValue();

            // Perform Transaction Processing with exception safety
            if (isCredit) {
                // Temporarily calculate new outstanding balance to verify limits
                double simulatedBalance = targetCustomer.getAccount().calculateBalance() + amount;
                
                if (simulatedBalance > targetCustomer.getCreditLimit()) {
                    // Credit Limit Breached! Catch with custom exception
                    try {
                        // Create a temporary mock to test limit alert throwing
                        Customer tempMock = new Customer(targetCustomer.getId(), targetCustomer.getName(), targetCustomer.getPhoneNumber(), targetCustomer.getEmail(), targetCustomer.getCreditLimit());
                        tempMock.getAccount().setTransactions(new ArrayList<>(targetCustomer.getAccount().getTransactions()));
                        tempMock.getAccount().addTransaction(amount, notes, "Credit", date, cat);
                        
                        // Enforce Exception contract (Alertable call)
                        tempMock.sendAlert();
                    } catch (CreditLimitExceededException cle) {
                        // EXCEPTION CAUGHT! Trigger override dialog (Decision 1 from User feedback flow)
                        boolean overrideApproved = openCreditBreachOverrideDialog(cle.getMessage());
                        if (!overrideApproved) {
                            lblError.setText("Transaction cancelled: Limit breach rejected.");
                            return;
                        }
                    }
                }

                // If override was approved or no breach, add the actual transaction
                targetCustomer.getAccount().addTransaction(amount, notes, "Credit", date, cat);
            } else {
                // Payment Transaction (No limit check needed)
                targetCustomer.getAccount().addTransaction(amount, notes, "Payment", date);
            }

            // Persist & Update UI
            StorageManager.saveData(customerList);
            loadLedgerForSelectedCustomer();
            refreshMetrics();
            dialog.close();
        });

        btnBox.getChildren().addAll(btnCancel, btnSave);

        form.getChildren().addAll(
            title, 
            new Label("Transaction Amount (Rs.) *"), txtAmount,
            new Label("Date *"), dpDate
        );

        if (isCredit) {
            form.getChildren().addAll(new Label("Product Category"), comboCategory);
        }

        form.getChildren().addAll(
            new Label("Description Notes"), txtNotes,
            lblError,
            btnBox
        );

        Scene scene = new Scene(form, 400, isCredit ? 520 : 440);
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // Modal dialog to capture the custom exception and ask for shopkeeper override approval
    private boolean overrideDecision = false;
    
    private boolean openCreditBreachOverrideDialog(String exceptionMessage) {
        overrideDecision = false;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("⚠ Credit Limit Breach Alert");

        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(15);
        root.getStyleClass().add("dialog-pane");

        Label title = new Label("⚠ EXCEPTION: CREDIT LIMIT EXCEEDED");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #b91c1c;");

        TextArea txtMessage = new TextArea(exceptionMessage);
        txtMessage.setEditable(false);
        txtMessage.setWrapText(true);
        txtMessage.setPrefHeight(100);
        txtMessage.setStyle("-fx-text-fill: #b91c1c; -fx-control-inner-background: #fee2e2; -fx-border-color: #fca5a5; -fx-background-radius: 8px; -fx-border-radius: 8px;");

        Label desc = new Label("ShopSense Exception Safeguard:\nDo you wish to bypass this credit limit threshold and approve this purchase anyway?");
        desc.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setSpacing(10);

        Button btnReject = new Button("⛔ Cancel Purchase");
        btnReject.getStyleClass().add("btn-danger");
        btnReject.setOnAction(e -> {
            overrideDecision = false;
            dialog.close();
        });

        Button btnOverride = new Button("✔️ Approve Override");
        btnOverride.getStyleClass().add("btn-success");
        btnOverride.setOnAction(e -> {
            overrideDecision = true;
            dialog.close();
        });

        btnBox.getChildren().addAll(btnReject, btnOverride);
        root.getChildren().addAll(title, txtMessage, desc, btnBox);

        Scene scene = new Scene(root, 480, 280);
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        dialog.setScene(scene);
        dialog.showAndWait();

        return overrideDecision;
    }

    // Receipt printing system
    private void generateAndExportReceipt(Transaction transaction) {
        if (selectedCustomerForLedger == null) return;
        Customer c = selectedCustomerForLedger;

        File receiptDir = new File("receipts");
        if (!receiptDir.exists()) {
            receiptDir.mkdir();
        }

        String fileName = String.format("receipts/receipt_%s_%s.txt", c.getId(), transaction.getTransactionId());
        
        try (FileWriter writer = new FileWriter(fileName)) {
            String border = "==================================================\n";
            String title = "            SHOPSENSE RETAIL SYSTEM               \n";
            String sub = "         Udhaar & Credit tracking system          \n";
            String dt = "  Date logged: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n";
            writer.write(border);
            writer.write(title);
            writer.write(sub);
            writer.write(border);
            writer.write(String.format("  INVOICE ID  : %s\n", transaction.getTransactionId()));
            writer.write(String.format("  CUSTOMER ID : %s\n", c.getId()));
            writer.write(String.format("  CLIENT NAME : %s\n", c.getName()));
            writer.write(String.format("  PHONE NUMBER: %s\n", c.getPhoneNumber()));
            writer.write(border);
            writer.write(String.format("  TX TYPE     : %s\n", transaction.getTransactionType().toUpperCase()));
            writer.write(String.format("  TX DATE     : %s\n", transaction.getDate()));
            
            if (transaction instanceof CreditTransaction) {
                writer.write(String.format("  CATEGORY    : %s\n", ((CreditTransaction) transaction).getCategory()));
            }

            writer.write(String.format("  DESCRIPTION : %s\n", transaction.getNotes().isEmpty() ? "General Retail entry" : transaction.getNotes()));
            writer.write(border);
            writer.write(String.format("  TRANSACTION AMOUNT  : Rs. %.2f\n", transaction.getAmount()));
            writer.write(String.format("  TOTAL ACCOUNT BAL   : Rs. %.2f\n", c.getAccount().calculateBalance()));
            writer.write(String.format("  CREDIT REMAINING    : Rs. %.2f\n", Math.max(0, c.getCreditLimit() - c.getAccount().calculateBalance())));
            writer.write(border);
            writer.write("          Thank you for choosing ShopSense!       \n");
            writer.write("            Software crafted in Java OOP          \n");
            writer.write(border);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Invoice Created");
            alert.setHeaderText("Receipt exported successfully!");
            alert.setContentText("A beautiful text-based printable invoice has been saved to your receipts folder:\n" + new File(fileName).getAbsolutePath());
            DialogPane dialogPane = alert.getDialogPane();
            File cssFile = new File("src/com/shopsense/ui/styles.css");
            if (cssFile.exists()) {
                dialogPane.getStylesheets().add(cssFile.toURI().toString());
            }
            dialogPane.getStyleClass().add("dialog-pane");
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Receipt Error");
            alert.setHeaderText("Unable to print invoice");
            alert.setContentText("Error details: " + e.getMessage());
            DialogPane dialogPane = alert.getDialogPane();
            File cssFile = new File("src/com/shopsense/ui/styles.css");
            if (cssFile.exists()) {
                dialogPane.getStylesheets().add(cssFile.toURI().toString());
            }
            dialogPane.getStyleClass().add("dialog-pane");
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
    }

    private void generateAndExportCompleteLedger(Customer customer) {
        if (customer == null) return;

        File receiptDir = new File("receipts");
        if (!receiptDir.exists()) {
            receiptDir.mkdir();
        }

        String fileName = String.format("receipts/ledger_%s.txt", customer.getId());
        
        try (FileWriter writer = new FileWriter(fileName)) {
            String doubleBorder = "================================================================================\n";
            String sectionDivider = "--------------------------------------------------------------------------------\n";
            String title = "                            SHOPSENSE RETAIL SYSTEM                             \n";
            String sub = "                       COMPLETE CUSTOMER ACCOUNT STATEMENT                      \n";
            String dt = "  Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n";
            
            writer.write(doubleBorder);
            writer.write(title);
            writer.write(sub);
            writer.write(doubleBorder);
            writer.write(dt);
            writer.write(doubleBorder);
            
            // Customer Details
            writer.write("  CUSTOMER PROFILE DETAILS\n");
            writer.write(sectionDivider);
            writer.write(String.format("  Customer ID     : %s\n", customer.getId()));
            writer.write(String.format("  Customer Name   : %s\n", customer.getName()));
            writer.write(String.format("  Phone Number    : %s\n", customer.getPhoneNumber()));
            writer.write(String.format("  Email Address   : %s\n", customer.getEmail().isEmpty() ? "N/A" : customer.getEmail()));
            writer.write(String.format("  Allowed Limit   : Rs. %.2f\n", customer.getCreditLimit()));
            
            double balance = customer.getAccount().calculateBalance();
            String statusStr = customer.checkCreditLimit() ? "⚠ EXCEEDED CREDIT LIMIT" : "✅ BALANCE HEALTHY";
            writer.write(String.format("  Account Status  : %s\n", statusStr));
            writer.write(doubleBorder);
            
            // Account Summary
            double totalCredit = 0;
            double totalPayment = 0;
            ArrayList<Transaction> transactions = customer.getAccount().getTransactions();
            for (Transaction t : transactions) {
                if (t instanceof CreditTransaction) {
                    totalCredit += t.getAmount();
                } else if (t instanceof PaymentTransaction) {
                    totalPayment += t.getAmount();
                }
            }
            
            writer.write("  ACCOUNT SUMMARY METRICS\n");
            writer.write(sectionDivider);
            writer.write(String.format("  Total Credit Purchases : Rs. %.2f\n", totalCredit));
            writer.write(String.format("  Total Payments Made    : Rs. %.2f\n", totalPayment));
            writer.write(String.format("  Current Balance Due    : Rs. %.2f\n", balance));
            writer.write(String.format("  Remaining Credit limit : Rs. %.2f\n", Math.max(0, customer.getCreditLimit() - balance)));
            writer.write(doubleBorder);
            
            // Transaction Register
            writer.write("  TRANSACTION HISTORY REGISTER (Chronological - Oldest to Newest)\n");
            writer.write(doubleBorder);
            
            // Table Headers
            writer.write(String.format("  %-11s | %-12s | %-8s | %-10s | %-12s | %-12s\n", "Date", "Transaction ID", "Type", "Category", "Amount", "Running Bal"));
            writer.write(sectionDivider);
            
            // Sort transactions chronologically (oldest to newest)
            ArrayList<Transaction> chronologicalTxs = new ArrayList<>(transactions);
            chronologicalTxs.sort(Comparator.comparing(Transaction::getDate));
            
            double runningBalance = 0.0;
            for (Transaction t : chronologicalTxs) {
                runningBalance += t.getEffectOnBalance();
                
                String cat = "-";
                if (t instanceof CreditTransaction) {
                    cat = ((CreditTransaction) t).getCategory();
                }
                
                String notes = t.getNotes().isEmpty() ? "Retail purchase/payment" : t.getNotes();
                
                writer.write(String.format("  %-11s | %-14s | %-8s | %-10s | Rs. %-8.2f | Rs. %-9.2f\n", 
                    t.getDate().toString(), 
                    t.getTransactionId(), 
                    t.getTransactionType(), 
                    cat, 
                    t.getAmount(), 
                    runningBalance
                ));
                writer.write(String.format("    └─ Notes: %s\n", notes));
                writer.write(sectionDivider);
            }
            
            writer.write(doubleBorder);
            writer.write("                       Thank you for choosing ShopSense!                        \n");
            writer.write("                         Statement Generated Digitally                          \n");
            writer.write(doubleBorder);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ledger Statement Exported");
            alert.setHeaderText("Complete Ledger Statement Exported Successfully!");
            alert.setContentText("A detailed chronological ledger history has been generated and saved to:\n" + new File(fileName).getAbsolutePath());
            DialogPane dialogPane = alert.getDialogPane();
            File cssFile = new File("src/com/shopsense/ui/styles.css");
            if (cssFile.exists()) {
                dialogPane.getStylesheets().add(cssFile.toURI().toString());
            }
            dialogPane.getStyleClass().add("dialog-pane");
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
            
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failure");
            alert.setHeaderText("Unable to export complete ledger");
            alert.setContentText("Error occurred during file operation: " + e.getMessage());
            DialogPane dialogPane = alert.getDialogPane();
            File cssFile = new File("src/com/shopsense/ui/styles.css");
            if (cssFile.exists()) {
                dialogPane.getStylesheets().add(cssFile.toURI().toString());
            }
            dialogPane.getStyleClass().add("dialog-pane");
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
        }
    }

    // ==========================================
    // MODULE 4: REPORTS & ANALYTICS VIEW
    // ==========================================
    private VBox reportDetailsBox = new VBox();
    
    private void setupReportsView() {
        reportsView.getChildren().clear();

        // Header
        VBox headerBox = new VBox();
        Label title = new Label("Financial Reporting & Business Analytics");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Categorized credit visualizers, top debtors, and balance highlights:");
        subtitle.getStyleClass().add("section-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        HBox chartsLayout = new HBox();
        chartsLayout.setSpacing(20);
        VBox.setVgrow(chartsLayout, Priority.ALWAYS);

        // Panel 1: Top 5 Outstanding Customer Balances
        VBox topDebtorsBox = new VBox();
        topDebtorsBox.setSpacing(15);
        topDebtorsBox.setPrefWidth(520);
        topDebtorsBox.getStyleClass().add("glass-panel");
        
        Label debtorsTitle = new Label("🏆 TOP 5 DEBTORS BY OUTSTANDING BALANCE");
        debtorsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-family: 'Poppins'; -fx-font-size: 14px;");
        
        VBox barChartLayout = new VBox();
        barChartLayout.setSpacing(18);
        barChartLayout.setPadding(new Insets(10, 0, 10, 0));

        // Get Top 5 sorted debtors using loop logic
        ArrayList<Customer> sortedCustomers = new ArrayList<>(customerList);
        sortedCustomers.sort((c1, c2) -> Double.compare(c2.getAccount().calculateBalance(), c1.getAccount().calculateBalance()));

        double maxBalance = 0;
        for (Customer c : sortedCustomers) {
            double bal = c.getAccount().calculateBalance();
            if (bal > maxBalance) maxBalance = bal;
        }

        int limitCount = Math.min(5, sortedCustomers.size());
        if (limitCount == 0) {
            barChartLayout.getChildren().add(new Label("No customer balances logged yet."));
        } else {
            for (int i = 0; i < limitCount; i++) {
                Customer c = sortedCustomers.get(i);
                double bal = c.getAccount().calculateBalance();
                
                VBox barRow = new VBox();
                barRow.setSpacing(5);

                HBox labels = new HBox();
                Label lblName = new Label((i + 1) + ". " + c.getName() + " (" + c.getId() + ")");
                lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-family: 'Inter';");
                
                Region rSpacer = new Region();
                HBox.setHgrow(rSpacer, Priority.ALWAYS);

                Label lblBalVal = new Label(String.format("Rs. %.2f", bal));
                lblBalVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF8C42; -fx-font-family: 'JetBrains Mono';");
                labels.getChildren().addAll(lblName, rSpacer, lblBalVal);

                // Progress-like visual bar for premium look
                ProgressBar pBar = new ProgressBar();
                double progress = maxBalance > 0 ? (bal / maxBalance) : 0;
                pBar.setProgress(progress);
                pBar.setMaxWidth(Double.MAX_VALUE);
                
                // Colorize bars beautifully (Breached red, normal blue)
                if (c.checkCreditLimit()) {
                    pBar.setStyle("-fx-accent: #ef4444;"); // Red bar
                } else {
                    pBar.setStyle("-fx-accent: #6366f1;"); // Indigo bar
                }

                barRow.getChildren().addAll(labels, pBar);
                barChartLayout.getChildren().add(barRow);
            }
        }
        topDebtorsBox.getChildren().addAll(debtorsTitle, barChartLayout);

        // Panel 2: Credit Categorization Breakdown
        VBox categoryBox = new VBox();
        categoryBox.setSpacing(15);
        categoryBox.getStyleClass().add("glass-panel");
        HBox.setHgrow(categoryBox, Priority.ALWAYS);

        Label categoryTitle = new Label("📊 OUTSTANDING CREDIT CATEGORY BREAKDOWN");
        categoryTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-family: 'Poppins'; -fx-font-size: 14px;");

        VBox categoryLayout = new VBox();
        categoryLayout.setSpacing(18);

        // Compute category balances
        double grocTotal = 0;
        double medTotal = 0;
        double houseTotal = 0;
        double otherTotal = 0;
        double overallCredit = 0;

        for (Customer c : customerList) {
            for (Transaction t : c.getAccount().getTransactions()) {
                if (t instanceof CreditTransaction) {
                    CreditTransaction ct = (CreditTransaction) t;
                    overallCredit += ct.getAmount();
                    switch (ct.getCategory()) {
                        case "Groceries": grocTotal += ct.getAmount(); break;
                        case "Medicines": medTotal += ct.getAmount(); break;
                        case "Household": houseTotal += ct.getAmount(); break;
                        default: otherTotal += ct.getAmount(); break;
                    }
                }
            }
        }

        if (overallCredit == 0) {
            categoryLayout.getChildren().add(new Label("No credit transactions recorded yet."));
        } else {
            categoryLayout.getChildren().addAll(
                createCategoryProgressRow("Groceries 🍎", grocTotal, overallCredit, "#10b981"), // Emerald
                createCategoryProgressRow("Medicines 💊", medTotal, overallCredit, "#3b82f6"), // Blue
                createCategoryProgressRow("Household 🏠", houseTotal, overallCredit, "#f59e0b"), // Orange
                createCategoryProgressRow("Others 📦", otherTotal, overallCredit, "#6366f1") // Indigo
            );
        }
        categoryBox.getChildren().addAll(categoryTitle, categoryLayout);

        chartsLayout.getChildren().addAll(topDebtorsBox, categoryBox);
        reportsView.getChildren().addAll(headerBox, chartsLayout);
    }

    private VBox createCategoryProgressRow(String title, double categoryAmount, double overallAmount, String colorHex) {
        VBox row = new VBox();
        row.setSpacing(5);

        HBox labels = new HBox();
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #5A6650; -fx-font-family: 'Inter';");

        Region rSpacer = new Region();
        HBox.setHgrow(rSpacer, Priority.ALWAYS);

        double pct = (categoryAmount / overallAmount) * 100;
        Label lblVal = new Label(String.format("Rs. %.2f (%.1f%%)", categoryAmount, pct));
        lblVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E2A14; -fx-font-family: 'JetBrains Mono';");
        labels.getChildren().addAll(lblTitle, rSpacer, lblVal);

        ProgressBar pBar = new ProgressBar();
        pBar.setProgress(categoryAmount / overallAmount);
        pBar.setMaxWidth(Double.MAX_VALUE);
        pBar.setStyle("-fx-accent: " + colorHex + ";");

        row.getChildren().addAll(labels, pBar);
        return row;
    }

    // Modal dialog to confirm exiting the application
    private void confirmAndExit(Stage ownerStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("Exit ShopSense");

        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(15);
        root.getStyleClass().add("dialog-pane");

        Label title = new Label("❌ Exit Application");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #D94F4F;");

        Label desc = new Label("Are you sure you want to close the ShopSense Credit Tracking System?");
        desc.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setSpacing(10);

        Button btnCancel = new Button("No, Stay");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnExit = new Button("Yes, Close");
        btnExit.getStyleClass().add("btn-danger");
        btnExit.setOnAction(e -> {
            dialog.close();
            ownerStage.close();
            System.exit(0);
        });

        btnBox.getChildren().addAll(btnCancel, btnExit);
        root.getChildren().addAll(title, desc, btnBox);

        Scene scene = new Scene(root, 420, 160);
        File cssFile = new File("src/com/shopsense/ui/styles.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}

