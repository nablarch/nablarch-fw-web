package nablarch.common.web.token;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import nablarch.TestUtil;
import org.junit.Test;

import nablarch.common.web.MockHttpSession;
import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.test.support.web.servlet.MockServletRequest;

/**
 * @author Kiyohito Itoh
 */
public class TokenUtilTest {

    @Test
    public void testIsValidTokenWithoutToken() {
        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                assertFalse("without token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForNoTokenParam() {
        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertFalse("no token param", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForNoTokenSession() {

        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, TokenTestUtil.TOKEN);
                assertFalse("no token session", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForInvalidToken() {

        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, "aaa");
                TokenTestUtil.setTokenSession(ctx, "bbb");
                assertFalse("invalid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenWithTokenParams() {

        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                WebConfig webConfig = WebConfigFinder.getWebConfig();
                req.getParamMap().put(webConfig.getDoubleSubmissionTokenParameterName(),
                        new String[] { "token1", "token2" });
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertFalse("invalid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForValidToken() {

        HttpServer server = TestUtil.createHttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, TokenTestUtil.TOKEN);
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertTrue("valid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), new ExecutionContext());
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testGenerateTokenWithoutConfig() {

        MockServletRequest nativeRequest = new MockServletRequest();
        MockHttpSession nativeSession = new MockHttpSession();
        nativeRequest.setSession(nativeSession);
        NablarchHttpServletRequestWrapper request = new NablarchHttpServletRequestWrapper(nativeRequest);

        WebConfig webConfig = WebConfigFinder.getWebConfig();

        assertNull(request.getAttribute(webConfig.getDoubleSubmissionTokenRequestAttributeName()));
        assertNull(nativeSession
                .getAttribute(webConfig.getDoubleSubmissionTokenSessionAttributeName()));

        String token = TokenUtil.generateToken(request);
        assertThat(token.length(), is(16));

        assertThat(request.getAttribute(webConfig.getDoubleSubmissionTokenRequestAttributeName())
                .toString(), is(token));
        assertThat(nativeSession
                .getAttribute(webConfig.getDoubleSubmissionTokenSessionAttributeName()).toString(),
                is(token));

        assertThat(TokenUtil.generateToken(request).length(), is(16));
    }

    @Test
    public void testGenerateTokenWithConfig() {

        TokenTestUtil.setUpTokenGenerator();

        MockServletRequest nativeRequest = new MockServletRequest();
        MockHttpSession nativeSession = new MockHttpSession();
        nativeRequest.setSession(nativeSession);
        NablarchHttpServletRequestWrapper request = new NablarchHttpServletRequestWrapper(nativeRequest);

        assertThat(TokenUtil.generateToken(request), is("token_test"));
    }
}
