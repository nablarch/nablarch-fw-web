package nablarch.common.web.session.store;

import mockit.Mocked;
import nablarch.common.web.session.MockHttpServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpSessionStore}のテスト。
 */
public class HttpSessionStoreTest {

    @Mocked
    private ServletContext unusedHttpContext;

    @Mocked
    private HttpServletResponse unusedHttpResponse;

    @Before
    public void setUp() {
        MockHttpServletRequest.sessionInvalidateCount = 0;
    }

    /**
     * HTTPセッションが存在しない場合、
     * delete呼び出しで、HttpSessionが作成されないこと。
     */
    @Test
    public void testDeleteWithNoHttpSession() {

        final HttpSessionStore sut = new HttpSessionStore();
        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();

        assertThat(
                "invalidate呼び出し前にHTTPセッションが存在しないこと",
                context.getServletRequest().getSession(false), is(nullValue()));

        sut.delete(unusedId, context);

        assertThat(
                "invalidate呼び出し後にもHTTPセッションが存在しないこと",
                context.getServletRequest().getSession(false), is(nullValue()));
    }

    /**
     * HTTPセッションが存在する場合、
     * delete呼び出しで、セッションの内容が削除されること。
     */
    @Test
    public void testDeleteWithHttpSession() {

        final HttpSessionStore sut = new HttpSessionStore();
        final String sid = "sid-1";
        final ServletExecutionContext context = createExeCtxt();

        // craete session
        context.getServletRequest().getSession(true);

        context.getServletRequest().getSession(true).setAttribute("sid-1", "sid-1-value");
        context.getServletRequest().getSession(true).setAttribute("sid-2", "sid-2-value");
        context.getServletRequest().getSession(true).setAttribute("sid-3", "sid-3-value");

        assertThat(
                "invalidate呼び出し前にHTTPセッションが存在すること",
                context.getServletRequest().getSession(false), is(notNullValue()));

        sut.delete("sid-2", context);

        assertThat(
                "invalidate呼び出し後にもHTTPセッションが存在すること",
                context.getServletRequest().getSession(false), is(notNullValue()));
        assertThat(
                "sid-1は存在すること",
                context.getServletRequest().getSession(false).getAttribute("sid-1").toString(), is("sid-1-value"));
        assertThat(
                "sid-2は存在しないこと",
                context.getServletRequest().getSession(false).getAttribute("sid-2"), is(nullValue()));
        assertThat(
                "sid-3は存在すること",
                context.getServletRequest().getSession(false).getAttribute("sid-3").toString(), is("sid-3-value"));
    }

    /**
     * HTTPセッションが存在しない場合、
     * invalidate呼び出しで、HttpSession#invalidateが呼ばれないこと。
     */
    @Test
    public void testInvalidateWithNoHttpSession() {

        final HttpSessionStore sut = new HttpSessionStore();
        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();

        assertThat(
                "invalidate呼び出し前にHTTPセッションが存在しないこと",
                context.getServletRequest().getSession(false), is(nullValue()));

        sut.invalidate(unusedId, context);

        assertThat(
                "invalidate呼び出し後にもHTTPセッションが存在しないこと",
                context.getServletRequest().getSession(false), is(nullValue()));
        assertThat(
                "HttpSession#invalidateが呼び出されていないこと",
                MockHttpServletRequest.sessionInvalidateCount, is(0));
    }

    /**
     * HTTPセッションが存在する場合、
     * invalidate呼び出しで、HttpSession#invalidateが呼ばれること。
     */
    @Test
    public void testInvalidateWithHttpSession() {

        final HttpSessionStore sut = new HttpSessionStore();
        final String unusedId = "unused";
        final ServletExecutionContext context = createExeCtxt();

        // craete session
        context.getServletRequest().getSession(true);

        assertThat(
                "invalidate呼び出し前にHTTPセッションが存在すること",
                context.getServletRequest().getSession(false), is(notNullValue()));

        sut.invalidate(unusedId, context);

        assertThat(
                "invalidateが呼び出されたので、HttpSessionが存在していないこと",
                context.getServletRequest().getSession(false), is(nullValue()));
        assertThat(
                "HttpSession#invalidateが呼び出されていること",
                MockHttpServletRequest.sessionInvalidateCount, is(1));
    }

    private ServletExecutionContext createExeCtxt() {
        return new ServletExecutionContext(
                new MockHttpServletRequest().getMockInstance(),
                unusedHttpResponse, unusedHttpContext);
    }
}
