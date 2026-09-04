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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StoreSystemTest {

    private static final String TEST_USER_ID = "junit_user_123";

    private Product shirt;
    private Branch branchTelAviv;

    @Before
    public void setUp() {
        shirt = new Product("P100", "Classic T-Shirt", "Shirts", 100.0);
        branchTelAviv = new Branch("B1", "Tel Aviv Main");
        branchTelAviv.addStock(shirt, 5);
        SessionManager.getInstance().logout(TEST_USER_ID);
    }

    @After
    public void tearDown() {
        SessionManager.getInstance().logout(TEST_USER_ID);
    }

    @Test
    public void testCustomerDiscounts() {
        Customer newCustomer = new NewCustomer("C100", "Israel Israeli", "123456789", "0501111111");
        Customer returningCustomer = new ReturningCustomer("C101", "Dana Levi", "234567891", "0502222222");
        Customer vipCustomer = new VipCustomer("C102", "David Cohen", "345678912", "0503333333");

        assertEquals(95.0, newCustomer.calculateFinalPrice(100.0), 0.001);
        assertEquals(90.0, returningCustomer.calculateFinalPrice(100.0), 0.001);
        assertEquals(80.0, vipCustomer.calculateFinalPrice(100.0), 0.001);
    }

    @Test
    public void testStockReductionSuccess() throws OutOfStockException {
        branchTelAviv.reduceStock(shirt, 3);

        Integer remaining = branchTelAviv.getInventorySnapshot().get(shirt);
        assertNotNull("The product must still be in inventory", remaining);
        assertEquals("Stock should be reduced to two items", 2, remaining.intValue());
    }

    @Test(expected = OutOfStockException.class)
    public void testOutOfStockExceptionThrown() throws OutOfStockException {
        branchTelAviv.reduceStock(shirt, 10);
    }

    @Test(expected = DuplicateLoginException.class)
    public void testDuplicateLoginPrevention() throws DuplicateLoginException {
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.login(TEST_USER_ID, null);
        sessionManager.login(TEST_USER_ID, null);
    }
}
