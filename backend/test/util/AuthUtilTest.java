package util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;
import static util.AuthUtil.isAlpha;
import static util.AuthUtil.isValidEmailAddress;

public class AuthUtilTest {

    @Test
    public void isAlphaFalse() {
        Assert.assertFalse(isAlpha("V1ka5"));
    }

    @Test
    public void isAlphaTrue() {
        Assert.assertTrue(isAlpha("Vikas"));
    }

    @Test
    public void isValidEmailAddressTrue() {
        Assert.assertTrue(isValidEmailAddress("testEmail@gmail.com"));
    }

    @Test
    public void isValidEmailAddressFalse() {
        Assert.assertFalse(isValidEmailAddress("testEmail.com"));
    }
}