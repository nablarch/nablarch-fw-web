package nablarch.fw.web.servlet;

import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * {@link PreventSessionCreationHttpServletRequestWrapper} のテストクラス。
 *
 * @author Tomoyuki Tanaka
 */
public class PreventSessionCreationHttpServletRequestWrapperTest {
    @Mocked
    public HttpServletRequest request;

    private PreventSessionCreationHttpServletRequestWrapper sut;

    @Before
    public void setUp() {
        sut = new PreventSessionCreationHttpServletRequestWrapper(this.request);
    }

    @Test
    public void testGetSessionWithFalseAlwaysReturnsNullValue() {
        HttpSession session = sut.getSession(false);
        assertThat(session, is(nullValue()));
    }

    @Test
    public void testGetSessionWithTrueAlwaysThrowsException() {
        try {
            sut.getSession(true);
            fail("例外がスローされなければならない");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Session creation is prevented."));
        }
    }

    @Test
    public void testGetSessionWithNoArgsAlwaysThrowsException() {
        try {
            sut.getSession();
            fail("例外がスローされなければならない");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Session creation is prevented."));
        }
    }
}