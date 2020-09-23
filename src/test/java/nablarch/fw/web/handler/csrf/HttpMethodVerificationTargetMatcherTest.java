package nablarch.fw.web.handler.csrf;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import nablarch.fw.web.MockHttpRequest;

/**
 * {@link HttpMethodVerificationTargetMatcher}のテストクラス。
 *
 */
public class HttpMethodVerificationTargetMatcherTest {

    private HttpMethodVerificationTargetMatcher sut;

    @Before
    public void init() {
        sut = new HttpMethodVerificationTargetMatcher();
    }

    @Test
    public void testDefaultAllowedMethods() {
        assertFalse("OPTIONS", sut.match(new MockHttpRequest().setMethod("OPTIONS")));
        assertFalse("GET", sut.match(new MockHttpRequest().setMethod("GET")));
        assertFalse("HEAD", sut.match(new MockHttpRequest().setMethod("HEAD")));
        assertTrue("POST", sut.match(new MockHttpRequest().setMethod("POST")));
        assertTrue("PUT", sut.match(new MockHttpRequest().setMethod("PUT")));
        assertTrue("DELETE", sut.match(new MockHttpRequest().setMethod("DELETE")));
        assertFalse("TRACE", sut.match(new MockHttpRequest().setMethod("TRACE")));
        assertTrue("CONNECT", sut.match(new MockHttpRequest().setMethod("CONNECT")));
    }

    @Test
    public void testSetAllowedMethods() throws Exception {
        Set<String> allowedMethods = new HashSet<String>();
        allowedMethods.add("POST");
        allowedMethods.add("PUT");
        allowedMethods.add("DELETE");
        allowedMethods.add("CONNECT");
        sut.setAllowedMethods(allowedMethods);

        assertTrue("OPTIONS", sut.match(new MockHttpRequest().setMethod("OPTIONS")));
        assertTrue("GET", sut.match(new MockHttpRequest().setMethod("GET")));
        assertTrue("HEAD", sut.match(new MockHttpRequest().setMethod("HEAD")));
        assertFalse("POST", sut.match(new MockHttpRequest().setMethod("POST")));
        assertFalse("PUT", sut.match(new MockHttpRequest().setMethod("PUT")));
        assertFalse("DELETE", sut.match(new MockHttpRequest().setMethod("DELETE")));
        assertTrue("TRACE", sut.match(new MockHttpRequest().setMethod("TRACE")));
        assertFalse("CONNECT", sut.match(new MockHttpRequest().setMethod("CONNECT")));
    }
}
