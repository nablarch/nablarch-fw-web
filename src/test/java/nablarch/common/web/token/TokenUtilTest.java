package nablarch.common.web.token;

import nablarch.common.web.handler.MockPageContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

        MockPageContext pageContext = new MockPageContext(true);

        assertNull(pageContext.getAttribute(TokenUtil.KEY_REQUEST_TOKEN));
        assertNull(pageContext.getAttribute(TokenUtil.KEY_SESSION_TOKEN, javax.servlet.jsp.PageContext.SESSION_SCOPE));

        String token = TokenUtil.generateToken(pageContext);
        assertThat(token.length(), is(16));

        assertThat(pageContext.getAttribute(TokenUtil.KEY_REQUEST_TOKEN, javax.servlet.jsp.PageContext.REQUEST_SCOPE).toString(), is(token));
        assertThat(pageContext.getNativeSession().getAttribute(TokenUtil.KEY_SESSION_TOKEN).toString(), is(token));

        assertThat(TokenUtil.generateToken(pageContext).length(), is(16));
    }

    @Test
    public void testGenerateTokenWithConfig() {

        TokenTestUtil.setUpTokenGenerator();

        MockPageContext pageContext = new MockPageContext(true);

        assertThat(TokenUtil.generateToken(pageContext), is("token_test"));
    }
}
