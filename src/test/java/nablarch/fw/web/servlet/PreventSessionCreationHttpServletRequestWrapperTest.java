package nablarch.fw.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * {@link PreventSessionCreationHttpServletRequestWrapper} のテストクラス。
 *
 * @author Tomoyuki Tanaka
 */
public class PreventSessionCreationHttpServletRequestWrapperTest {
    public HttpServletRequest request = mock(HttpServletRequest.class);

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