package nablarch.common.web.token;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import org.junit.Test;

import nablarch.common.web.MockHttpSession;
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
        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                assertFalse("without token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForNoTokenParam() {
        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertFalse("no token param", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForNoTokenSession() {

        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, TokenTestUtil.TOKEN);
                assertFalse("no token session", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForInvalidToken() {

        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, "aaa");
                TokenTestUtil.setTokenSession(ctx, "bbb");
                assertFalse("invalid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenWithTokenParams() {

        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                req.getParamMap().put(TokenUtil.KEY_HIDDEN_TOKEN, new String[] { "token1", "token2" });
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertFalse("invalid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testIsValidTokenForValidToken() {

        HttpServer server = new HttpServer().addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setTokenParam(req, TokenTestUtil.TOKEN);
                TokenTestUtil.setTokenSession(ctx, TokenTestUtil.TOKEN);
                assertTrue("valid token", TokenUtil.isValidToken(req, ctx));
                return new HttpResponse(200);
            }
        }).startLocal();

        HttpResponse res = server.handle(new MockHttpRequest(), null);
        assertEquals(200, res.getStatusCode());
    }

    @Test
    public void testGenerateTokenWithoutConfig() {

        MockServletRequest nativeRequest = new MockServletRequest();
        MockHttpSession nativeSession = new MockHttpSession();
        nativeRequest.setSession(nativeSession);
        NablarchHttpServletRequestWrapper request = new NablarchHttpServletRequestWrapper(nativeRequest);

        assertNull(request.getAttribute(TokenUtil.KEY_REQUEST_TOKEN));
        assertNull(nativeSession.getAttribute(TokenUtil.KEY_SESSION_TOKEN));

        String token = TokenUtil.generateToken(request);
        assertThat(token.length(), is(16));

        assertThat(request.getAttribute(TokenUtil.KEY_REQUEST_TOKEN).toString(), is(token));
        assertThat(nativeSession.getAttribute(TokenUtil.KEY_SESSION_TOKEN).toString(), is(token));

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
