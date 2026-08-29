package test;

import exceptions.DuplicateLoginException;
import exceptions.OutOfStockException;
import model.Branch;
import model.Product;
import model.customers.Customer;
import model.customers.NewCustomer;
import model.customers.ReturningCustomer;
import model.customers.VipCustomer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.SessionManager;

import static org.junit.Assert.*;

public class StoreSystemTest {

    private Product shirt; // המוצר שנשתמש בו בבדיקות
    private Branch branchTelAviv; // הסניף שבו נבדוק את ניהול המלאי

    // לפני כל בדיקה, נוודא שהסשן נקי ושיש לנו את המוצר והסניף הנדרשים
    @Before
    public void setUp() {
        shirt = new Product("P100", "Classic T-Shirt", "Shirts", 100.0);
        branchTelAviv = new Branch("B1", "Tel Aviv Main");
        branchTelAviv.addStock(shirt, 5);

        // מוודא שהסשן נקי לפני כל בדיקה
        SessionManager.getInstance().logout("user_123");
    }

    // לאחר כל בדיקה, נוודא שהמשתמש מתנתק כדי למנוע השפעות בין בדיקות
    @After
    public void tearDown() {
        SessionManager.getInstance().logout("user_123");
    }

    // בדיקות של חישוב הנחות ללקוחות
    @Test
    public void testCustomerDiscounts() {
        Customer newCust = new NewCustomer("1", "Israel Israeli", "0501111111");
        Customer returningCust = new ReturningCustomer("2", "Dana Levi", "0502222222");
        Customer vipCust = new VipCustomer("3", "David Cohen", "0503333333");

        assertEquals(95.0, newCust.calculateFinalPrice(100.0), 0.001);
        assertEquals(90.0, returningCust.calculateFinalPrice(100.0), 0.001);
        assertEquals(80.0, vipCust.calculateFinalPrice(100.0), 0.001);
    }

    // בדיקות של ניהול מלאי והפחתת מלאי
    @Test
    public void testStockReductionSuccess() throws OutOfStockException {
        branchTelAviv.reduceStock(shirt, 3);
        Integer remaining = branchTelAviv.getInventorySnapshot().get(shirt);

        assertNotNull("המוצר חייב להיות קיים במלאי", remaining);
        assertEquals("הכמות במלאי צריכה לרדת ל-2", 2, remaining.intValue());
    }

    @Test(expected = OutOfStockException.class)
    public void testOutOfStockExceptionThrown() throws OutOfStockException {
        // מנסה למשוך 10 פריטים כשבמלאי יש רק 5 - אמור לזרוק חריגה
        branchTelAviv.reduceStock(shirt, 10);
    }

    // בדיקה של מניעת כניסה כפולה
    @Test(expected = DuplicateLoginException.class)
    public void testDuplicateLoginPrevention() throws DuplicateLoginException {
        SessionManager sessionManager = SessionManager.getInstance();

        sessionManager.login("user_123", null);

        sessionManager.login("user_123", null);
    }
}