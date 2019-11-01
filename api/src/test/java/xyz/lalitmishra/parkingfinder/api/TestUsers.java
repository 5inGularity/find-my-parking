package xyz.lalitmishra.parkingfinder.api;

import org.junit.Assert;
import org.junit.Test;
import xyz.lalitmishra.parkingfinder.api.data.User;

public class TestUsers extends TestAPI {
    @Test
    public void testGetUsers() {
        User[] users = template.getForObject(base() + "/users", User[].class);
        Assert.assertEquals(2, users.length);

        Assert.assertTrue("1234567890".equals(users[0].getPhoneNumber())
                || "1234567890".equals(users[1].getPhoneNumber()));
        Assert.assertTrue("0987654321".equals(users[0].getPhoneNumber())
                || "0987654321".equals(users[1].getPhoneNumber()));
    }
}
