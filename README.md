# Electricity Invoice Management System

## 📋 Project Overview

The **Electricity Invoice Management System** is a comprehensive enterprise-level web application designed to streamline and automate the management of electricity invoices for multiple electricity providers and their customers. This system provides a secure, role-based platform that handles invoice creation, tracking, payment management, pricing history, and advanced auditing capabilities.

---

## 🎯 Project Purpose and Benefits

### Main Objectives
1. **Centralized Invoice Management**: Manage all electricity invoices for multiple providers in one unified system
2. **Role-Based Access Control**: Ensure data security through strict role-based permissions
3. **Automated Tracking**: Track invoice lifecycle from creation to payment
4. **Pricing Transparency**: Maintain complete history of electricity pricing changes
5. **Advanced Auditing**: Provide comprehensive audit trails and AI-powered analytics
6. **Multi-Provider Support**: Support multiple electricity providers with isolated data access

### Key Benefits
- **For Providers**: Efficient invoice management, automated calculations, pricing control
- **For Customers**: Easy access to invoices, payment tracking, transparent billing
- **For Auditors**: Complete visibility into operations with AI-powered query capabilities
- **For Administrators**: Full system control, user management, provider configuration

---

## 🏗️ System Architecture

### Technology Stack

#### Backend Technologies
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: MySQL 8
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security with JWT Authentication
- **API Documentation**: Swagger/OpenAPI (SpringDoc)
- **AI Integration**: Google Gemini API (Text-to-SQL)
- **Build Tool**: Maven

#### Key Dependencies
- **JWT**: JSON Web Tokens for stateless authentication (io.jsonwebtoken)
- **Lombok**: Reduce boilerplate code
- **WebFlux**: Reactive web client for external API calls
- **Validation**: Jakarta Bean Validation
- **MySQL Connector**: Database connectivity

### Architecture Pattern
- **Layered Architecture**: Controller → Service → Repository → Entity
- **RESTful API**: Standard REST endpoints for all operations
- **Stateless Authentication**: JWT-based security
- **Multi-Tenancy**: Provider-based data isolation

---

## 👥 User Roles and Permissions

The system implements **5 distinct user roles**, each with specific permissions:

### 1. **Customer** 👤
- View their own electricity invoices
- Check payment status
- Access invoice details and history
- **Restrictions**: Cannot create or modify invoices

### 2. **Invoice Creator** 📝
- Create new invoices for customers
- Update invoices they created
- View invoices they created
- **Restrictions**: Limited to their own created invoices

### 3. **Super Creator** 🔧
- All Invoice Creator permissions
- View ALL invoices for their provider
- Manage invoices across the entire provider
- **Restrictions**: Limited to their assigned provider only

### 4. **Auditor** 🔍
- **Read-only access** to all provider data
- View all invoices from their provider
- Access complete audit logs
- Search invoices by number
- View pricing history
- **AI-Powered Queries**: Ask questions in natural language (Text-to-SQL using Gemini AI)
- **Restrictions**: Cannot create, update, or delete any data

### 5. **Admin** 👑
- **Full system access**
- Create and manage electricity providers
- Create customer accounts with **role assignment**
- Update electricity pricing (kWh rates)
- Manage user roles (assign/remove)
- Manage provider information
- **Restrictions**: None - highest privilege level

---

## 🔄 Multi-Role System

### Overview
The system now supports **multiple roles per user**, allowing flexible permission management and role-based workflows.

### Key Features

#### 1. **Multiple Roles Per User**
- Users can have **one or more roles** simultaneously
- Example: A user can be both `Customer` and `Auditor`
- Stored in separate `user_roles` table with many-to-many relationship

#### 2. **Role Selection on Login** 🎯
When a user with **multiple roles** logs in:
1. System detects multiple roles
2. Displays **Role Selection Page** in frontend
3. User selects their desired role for the session
4. JWT token is issued with the selected role
5. Dashboard loads based on selected role

**For single-role users**: Login proceeds directly to dashboard (no selection needed)

#### 3. **Role Switching** 🔄
Users with multiple roles can **switch roles without re-login**:
- Click "Switch Role" button in navigation bar
- Select different role from dropdown
- New JWT token issued instantly
- Dashboard updates automatically
- No need to logout and login again

#### 4. **Admin Role Management** 👑

##### Create User with Roles
When Admin creates a new user:
- Selects **one or more roles** via checkboxes
- Roles: Customer, Invoice_Creator, Super_Creator, Auditor, Admin
- If no roles selected → defaults to `Customer`

##### Manage Existing User Roles
- **Assign Role**: `POST /api/admin/users/roles`
- **Remove Role**: `DELETE /api/admin/users/{userId}/roles/{role}`
- **View User Roles**: `GET /api/admin/users/{userId}/roles`
- **Protection**: Cannot remove last role (user must have at least 1 role)

### API Endpoints

#### Authentication with Multi-Role
```
POST /api/auth/login
Request: { "email": "user@example.com", "password": "password" }

Response (Multiple Roles):
{
  "token": null,
  "userId": 1,
  "name": "John Doe",
  "email": "user@example.com",
  "roles": ["Admin", "Customer"],
  "selectedRole": null,
  "requiresRoleSelection": true
}

Response (Single Role):
{
  "token": "eyJhbGc...",
  "userId": 1,
  "roles": ["Customer"],
  "selectedRole": "Customer",
  "requiresRoleSelection": false
}
```

#### Role Selection
```
POST /api/auth/select-role
Request: { "userId": 1, "selectedRole": "Admin" }
Response: { "token": "eyJhbGc...", "selectedRole": "Admin", ... }
```

#### Role Switching
```
POST /api/auth/switch-role
Request: { "newRole": "Customer" }
Response: { "token": "eyJhbGc...", "selectedRole": "Customer", ... }
```

#### Role Management (Admin Only)
```
POST /api/admin/users/roles
Request: { "userId": 5, "role": "Auditor" }

DELETE /api/admin/users/{userId}/roles/{role}
Example: DELETE /api/admin/users/5/roles/Customer

GET /api/admin/users/{userId}/roles
Response: ["Customer", "Auditor", "Admin"]
```

### Frontend Features

#### 1. **Role Selection Page**
- Beautiful card-based interface
- Role icons and descriptions
- Displays after login for multi-role users
- Click to select → Dashboard loads

#### 2. **Role Switcher Component**
- Located in navigation bar
- Visible only for multi-role users
- Dropdown with all available roles
- Highlights current active role
- Instant role switching

#### 3. **Admin User Creation Form**
- Checkbox group for role selection
- Visual role indicators with emojis
- Compact, responsive design
- Supports selecting multiple roles

### Database Schema Changes

#### New Table: `user_roles`
```sql
CREATE TABLE user_roles (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  role VARCHAR(50) NOT NULL,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES User(user_id),
  UNIQUE KEY unique_user_role (user_id, role)
);
```

#### Modified Table: `User`
- **Removed**: `role` column (deprecated)
- **Added**: `@OneToMany` relationship to `user_roles`
- Lazy loading for optimal performance

### Security Considerations

1. **JWT Token Contains Selected Role**
   - Only one role active per session
   - Token validates against selected role
   - Role switching issues new token

2. **Role Validation**
   - Every endpoint checks selected role
   - User must have the role they're using
   - Cannot impersonate roles they don't have

3. **Last Role Protection**
   - System prevents removing user's last role
   - Ensures users always have access
   - Error: "Cannot remove the last role"

### Migration Guide

For existing users in the database:
```sql
-- Migrate existing roles to user_roles table
INSERT INTO user_roles (user_id, role)
SELECT user_id, role FROM User WHERE role IS NOT NULL;

-- Then remove old role column
ALTER TABLE User DROP COLUMN role;
```

---

## ✨ Core Features

### 1. Authentication & Security 🔐
- **JWT-based authentication** with 7-day token expiration
- **Role-based access control** (RBAC)
- **Password encryption** for secure storage
- **Token validation** on every request
- **CORS configuration** for cross-origin requests
- **Stateless sessions** for scalability

### 2. Invoice Management 📄

#### Invoice Creation
- Create invoices with customer details
- Automatic calculation: `Total Amount = kWh Consumed × Current Price`
- Link to current pricing at time of creation
- Assign invoice numbers
- Set issue dates and due dates

#### Invoice Tracking
- **Payment Status**: Pending, Paid, Overdue, Cancelled
- Track payment dates
- View invoice history
- Search by invoice number

#### Invoice Fields
- Invoice Number (unique)
- Customer Information
- Provider Information
- kWh Consumed
- Total Amount
- Issue Date & Due Date
- Payment Status & Payment Date
- Creator Information
- Pricing Reference

### 3. Provider Management 🏢
- Create multiple electricity providers
- Each provider has:
  - Name, City, Email, Phone
  - Current kWh Price
  - Active/Inactive status
- Update pricing with automatic history tracking
- Provider-based data isolation

### 4. Pricing History 💰
- **Automatic tracking** of all price changes
- Records:
  - Previous and new kWh prices
  - Who made the change
  - When it was changed (valid_from, valid_to)
  - Provider association
- **Historical accuracy**: Invoices reference the price active at creation time

### 5. Audit System 📊

#### Automatic Audit Logging
- **Every invoice operation is logged**:
  - CREATE: New invoice creation
  - UPDATE: Invoice modifications
  - DELETE: Invoice deletions (if implemented)
- **Audit Log Contains**:
  - Action type
  - Who performed it
  - When it was performed
  - Old values (JSON)
  - New values (JSON)
  - Associated invoice

#### Audit Capabilities
- View all audit logs for provider
- Search logs by invoice number
- View complete change history for specific invoices
- Track who created/modified each invoice

### 6. AI-Powered Analytics 🤖

#### Natural Language Queries (Gemini AI Integration)
Auditors can ask questions in **plain English**, and the system:
1. Converts the question to SQL using **Google Gemini AI**
2. Validates the query for security
3. Executes it against the database
4. Returns results in JSON format

#### Example Queries
- "Show me all unpaid invoices from last month"
- "What is the total revenue for January 2025?"
- "List customers with overdue payments"
- "How many invoices were created by each employee?"

#### Security Features
- **Only SELECT queries** allowed (no INSERT/UPDATE/DELETE)
- **Automatic provider filtering**: Auditors only see their provider's data
- **SQL injection prevention**
- **Result limit**: Maximum 100 rows

---

## 🔄 System Workflow

### Typical Usage Flow

```
1. Admin Setup
   ├─ Create Provider (e.g., "City Electric Company")
   ├─ Set initial kWh price
   └─ Create users (Customers, Creators, Auditors)

2. Invoice Creation
   ├─ Invoice Creator logs in
   ├─ Creates invoice for customer
   │  ├─ Enters kWh consumed
   │  ├─ System calculates total (kWh × price)
   │  └─ Sets due date
   └─ Audit log automatically created

3. Customer Access
   ├─ Customer logs in
   ├─ Views their invoices
   └─ Checks payment status

4. Payment Processing
   ├─ Invoice Creator/Super Creator updates status
   ├─ Marks as "Paid" with payment date
   └─ Audit log records the change

5. Auditing
   ├─ Auditor logs in
   ├─ Views all provider invoices
   ├─ Checks audit logs
   └─ Asks AI-powered questions

6. Price Updates
   ├─ Admin updates kWh price
   ├─ System creates pricing history record
   └─ New invoices use new price
```

---

## 📊 Database Schema

### Main Tables

1. **Provider**
   - provider_id, name, city, email, phone_number
   - current_kwh_price, is_active
   - created_at, updated_at

2. **User**
   - user_id, provider_id, name, email, password
   - phone_number, address
   - created_at, updated_at, is_active
   - **Note**: `role` column removed (deprecated)

3. **user_roles**
   - id, user_id, role
   - assigned_at
   - UNIQUE constraint on (user_id, role)

4. **Invoice**
   - invoice_id, customer_id, provider_id
   - created_by_user_id, pricing_id
   - invoice_number, kwh_consumed, total_amount
   - issue_date, due_date, payment_status, payment_date
   - created_at

4. **Pricing_History**
   - pricing_id, provider_id, changed_by_user_id
   - kwh_price, valid_from, valid_to
   - created_at

5. **Audit_logs**
   - audit_id, invoice_id, performed_by_user_id
   - action, old_value, new_value
   - performed_at

### Relationships
- Provider → Users (1:N)
- Provider → Invoices (1:N)
- Provider → Pricing History (1:N)
- User (Customer) → Invoices (1:N)
- User (Creator) → Invoices Created (1:N)
- Invoice → Audit Logs (1:N)
- Pricing History → Invoices (1:N)

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/login` - User login (returns JWT token)

### Admin Endpoints (Role: Admin)
- `POST /api/admin/users` - Create customer
- `POST /api/admin/providers` - Create provider
- `PUT /api/admin/providers/{id}/price` - Update kWh price
- `GET /api/admin/providers/{id}` - Get provider details

### Invoice Endpoints
- `POST /api/invoices` - Create invoice (Invoice_Creator, Super_Creator)
- `PUT /api/invoices/{id}` - Update invoice (Invoice_Creator, Super_Creator)
- `GET /api/invoices/{id}` - Get invoice by ID
- `GET /api/invoices/my-invoices` - Customer's invoices (Customer)
- `GET /api/invoices/my-created` - Creator's invoices (Invoice_Creator)
- `GET /api/invoices/provider` - All provider invoices (Super_Creator)

### Audit Endpoints (Role: Auditor)
- `GET /api/audit/invoices` - Get all provider invoices (read-only)
- `GET /api/audit/invoices/search` - Search by invoice number
- `GET /api/audit/logs` - Get all audit logs
- `GET /api/audit/logs/search` - Search audit logs by invoice number
- `GET /api/audit/invoices/{id}/history` - Get invoice change history
- `POST /api/audit/query` - Natural language query (AI-powered)
- `GET /api/audit/pricing-history` - Get pricing history

---

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- MySQL 8.0 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Electricity_Invoice_Management
   ```

2. **Configure Database**
   - Create MySQL database:
     ```sql
     CREATE DATABASE electricity_management;
     ```
   - Update `application.properties`:
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/electricity_management
     spring.datasource.username=your_username
     spring.datasource.password=your_password
     ```

3. **Configure Gemini API (Optional - for AI features)**
   - Get API key from: https://aistudio.google.com/app/apikey
   - Add to `application.properties`:
     ```properties
     gemini.api.key=your_api_key_here
     ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**
   - API: http://localhost:8081
   - Swagger UI: http://localhost:8081/swagger-ui.html

---

## 📖 API Documentation

The system includes **interactive API documentation** using Swagger/OpenAPI:
- **URL**: http://localhost:8081/swagger-ui.html
- **Features**:
  - View all endpoints
  - Test API calls directly
  - See request/response schemas
  - Authentication support

---

## 🔒 Security Features

1. **JWT Authentication**
   - Tokens expire after 7 days
   - Secure token generation with HMAC-SHA
   - Token contains: username, role, userId

2. **Role-Based Access Control**
   - Endpoint-level security
   - Method-level security with `@PreAuthorize`
   - Automatic role validation

3. **Data Isolation**
   - Provider-based data separation
   - Users only access their provider's data
   - Automatic filtering in queries

4. **Password Security**
   - Encrypted password storage
   - Secure authentication flow

5. **SQL Injection Prevention**
   - Parameterized queries
   - JPA/Hibernate protection
   - AI query validation

---

## 🎨 Key Innovations

### 1. **Multi-Role System with Dynamic Role Switching** 🔄
- Users can have **multiple roles simultaneously**
- **Seamless role switching** without re-login
- Dynamic JWT token generation per role
- Beautiful role selection UI
- Admin-controlled role assignment

### 2. AI-Powered Auditing
- **First-of-its-kind** natural language query system for invoice auditing
- Converts plain English to SQL using Google Gemini AI
- Secure, provider-scoped query execution

### 3. Automatic Audit Trails
- Every change is automatically logged
- Complete before/after snapshots in JSON
- Full traceability for compliance

### 4. Historical Pricing Accuracy
- Invoices always reference the correct historical price
- Complete pricing change history
- Prevents billing disputes

### 5. Multi-Provider Architecture
- Single system supports unlimited providers
- Complete data isolation
- Scalable design

---

## 📈 Use Cases

### 1. Small Electricity Provider
- Manage 1,000+ customers
- Track monthly invoices
- Monitor payments
- Generate reports

### 2. Multi-City Provider Network
- Multiple providers in different cities
- Centralized management
- Provider-specific pricing
- Consolidated auditing

### 3. Regulatory Compliance
- Complete audit trails
- Historical data preservation
- Transparent pricing changes
- Compliance reporting

### 4. Customer Service
- Quick invoice lookup
- Payment verification
- Dispute resolution
- Historical billing review

---

## 🛠️ Configuration

### Application Properties

```properties
# Server Configuration
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/electricity_management
spring.datasource.username=root
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Configuration
jwt.secret=your_secret_key_here
jwt.expiration=604800000  # 7 days

# Gemini AI Configuration
gemini.api.key=your_gemini_api_key
gemini.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=

# Logging
logging.level.Electricity.Management.security=DEBUG
```

---


## 📄 License

This project is developed as a training project at **Harri** for electricity invoice management.

