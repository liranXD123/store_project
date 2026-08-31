# Store Network Management System

A client–server system for managing a clothing store network: branch inventory, network customers,
employees, sales reports and chat between branches.

## Requirements

- JDK 11 or newer (developed on JDK 26)
- No external libraries — everything compiles with `javac` alone

## Build

Run from the project root:

```bash
javac -d out $(find src -name "*.java")
```

On Windows PowerShell:

```powershell
javac -d out (Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
```

## Run

The server must be started first. Use **two terminals**, both opened at the project root.

Terminal 1 — the server:

```bash
java -cp out server.StoreServer
```

Terminal 2 — a client:

```bash
java -cp out client.StoreClient
```

Open more terminals with the same client command to connect more employees at the same time.
The server listens on port **7000**.

On the first run the server creates a `db/` folder with the starting data and prints:

```
Store Management Server is starting on port 7000...
Created the database folder: db
Created branches.json with the starting branches.
Created users.json with the starting employees.
Created customers.json with the starting customers.
Created products.json with the starting products and stock.
Created sales.json with an empty sales history.
Database loaded: 2 branches, 3 employees, 3 customers, 3 products, 0 sales.
```

Every later run reads that folder, so employees, customers, stock and sales stay as you left them.
To start over from the original data, delete the `db/` folder and run the server again.

## Log in

| Employee ID | Password   | Role          | Branch |
|-------------|------------|---------------|--------|
| `E101`      | `admin123` | Admin         | B1     |
| `E102`      | `mgr123`   | Shift Manager | B1     |
| `E103`      | `cash123`  | Cashier       | B2     |

The same employee cannot be logged in from two clients at once. Log out of the first one before
using the second.

## Using the client

Type the number of the option you want and press Enter. Two words work at any time:

- `menu` — show the menu again
- `exit` — leave the system

Before logging in only **Login** and **Logout & Exit** are offered. After logging in the menu is
rebuilt for your role, so the numbers change — type `menu` if you are unsure.

### Cashier and Seller

```
1. View Branch Inventory
2. Process Sale
3. View All Customers
4. Add New Customer
5. Generate JSON Report (Branch/ALL)
6. Export Word Report (Branch/ALL)
7. Request Chat with another branch
8. Send Chat Message
9. Logout & Exit
```

### Shift Manager

Everything above, plus:

```
9.  [MANAGER] Restock Product (buy into branch)
10. [MANAGER] View Active Chats in Branch
11. [MANAGER] Join Active Chat in Branch
```

### Admin

Everything above, plus:

```
12. [ADMIN] View All Employees
13. [ADMIN] Add New Employee
```

## Starting data

**Branches**

| ID   | Name     |
|------|----------|
| `B1` | Tel Aviv |
| `B2` | Haifa    |

**Products**

| ID    | Name           | Category | Price | Stock in B1 | Stock in B2 |
|-------|----------------|----------|-------|-------------|-------------|
| `P01` | Polo Shirt     | Shirts   | 120   | 20          | 10          |
| `P02` | Jeans          | Pants    | 250   | 15          | —           |
| `P03` | Leather Jacket | Jackets  | 450   | —           | 8           |

**Customers**

| ID    | Name         | Type        | Discount |
|-------|--------------|-------------|----------|
| `C01` | Ronnie Kline | `NEW`       | 5%       |
| `C02` | Michal Ziv   | `RETURNING` | 10%      |
| `C03` | Alon Doron   | `VIP`       | 20%      |

## Walkthroughs

### Make a sale

1. Log in as `E103` / `cash123` (branch B2).
2. Choose **View Branch Inventory** to see what B2 has in stock.
3. Choose **Process Sale** and enter a customer ID, a product ID and a quantity — for example
   `C03`, `P01`, `2`.
4. The price shown is already reduced according to the customer's type.

Every employee of that branch is told that the stock changed.

### Restock a branch

1. Log in as `E102` / `mgr123` (branch B1).
2. Choose **Restock Product**, then enter a product ID and how many units arrived — for example
   `P02` and `10`.

Stock is always added to the branch you belong to.

### Add a customer

Choose **Add New Customer** and enter a customer ID, full name, ID number (T.Z), phone, and the
type: `NEW`, `RETURNING` or `VIP`. The type decides the discount that customer gets on every
future purchase. All connected employees are told that the customer list changed.

### Add an employee

Log in as `E101` / `admin123` and choose **Add New Employee**. You will be asked for an employee
ID, full name, ID number (T.Z), phone, bank account, branch, role and password.

- The branch must already exist — `B1` or `B2`
- The role must be `ADMIN`, `SHIFT_MANAGER`, `CASHIER` or `SELLER`
- The password must be at least 6 characters and contain an upper case letter, a lower case
  letter and a digit — for example `Abcdef1`

### Produce a report

Choose **Generate JSON Report** to read it on screen, or **Export Word Report** to save it as a
document. Both then ask what the report should cover:

```
1. ALL       the whole network
2. BRANCH    one branch, e.g. B1
3. PRODUCT   one product, e.g. P01
4. CATEGORY  one category, e.g. Shirts
5. DATE      one day, written as yyyy-MM-dd
```

Every report also breaks the sales down per branch. The Word report is saved next to the server as
`Sales_Report_<filter>_<number>.doc` and opens in Word or in any browser.

### Chat with another branch

1. Log in on two clients as employees of different branches — for example `E101` (B1) and
   `E103` (B2).
2. On the first client choose **Request Chat with another branch** and enter the other branch,
   e.g. `B2`.
3. The server connects you to an employee of that branch who is free, and both sides see
   `Conversation started with ...`.
4. Choose **Send Chat Message** to start typing. Everything you type is sent as a message.
5. Type `/exit` to leave the conversation and return to the menu.

If nobody in that branch is free, you are placed in a waiting queue. As soon as one of that
branch's employees is free, they are told that you tried to reach them and can open a chat back.

### Follow a chat as a manager

1. Log in as `E102` / `mgr123`.
2. Choose **View Active Chats in Branch** to see which employees of your branch are talking and
   who they are talking to.
3. Choose **Join Active Chat in Branch** and enter the employee ID from that list.
4. Type `/exit` when you are done.

A shift manager can join the conversations of their own branch. An admin can join any of them.

## Files the system writes

Both folders are created next to wherever you started the server.

```
db/                     the database, kept as JSON
  branches.json
  users.json
  customers.json
  products.json         products and the stock of each branch
  sales.json
logs/                   one file per kind of action
  employees.log
  customers.log
  sales_transactions.log
  chat_history.log
  system.log
```
