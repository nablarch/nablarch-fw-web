package nablarch.common.web.token;

import nablarch.core.message.ApplicationException;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.HttpErrorHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Kiyohito Itoh
 */
@SuppressWarnings("unused")
public class OnDoubleSubmissionTest {

    @BeforeClass
    public static void classSetup() throws Exception {
        TokenTestUtil.setUpMessageBeforeClass();
    }

    @AfterClass
    public static void classDown() throws Exception {
        TokenTestUtil.tearDownAfterClass();
    }

    private HttpRequest request;

    @Before
    public void setUp() {
        request = new MockHttpRequest("GET /index.html HTTP/1.1");
    }

    @After
    public void tearDown() {
        SystemRepository.clear();
    }

    @Test
    public void testForDefaultWithValidToken() {

        final List<Boolean> asserted = new ArrayList<Boolean>();

        HttpServer server = new HttpServer()
        .addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setUpValidToken(req, ctx);
                TokenTestUtil.setUpMessage(false);
                HttpResponse response = ctx.handleNext(req);

                assertThat(response.getContentPath().toString(), is("servlet:///complete.jsp"));
                assertThat(response.getStatusCode(), is(200));
                assertNull(ctx.getRequestScopedVar("nablarch_error"));
                asserted.add(true);
                return response;
            }
        })
        .addHandler(new Object() {
            @OnDoubleSubmission(path="/input.jsp", messageId="MSG00011")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse(200, "/complete.jsp");
            }
        }).startLocal();

        server.handle(request, null);
        assertEquals(1, asserted.size());
    }

    @Test
    public void testForDefaultWithInvalidToken() {

        final List<Boolean> asserted = new ArrayList<Boolean>();

        HttpServer server = new HttpServer()
        .addHandler(new HttpErrorHandler())
        .addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx)  {
                TokenTestUtil.setUpInvalidToken(req, ctx);
                TokenTestUtil.setUpMessage(false);

                try {
                    HttpResponse response = ctx.handleNext(req);
                    fail();
                    return response;

                } catch (HttpErrorResponse e) {
                    ApplicationException ae = (ApplicationException) e.getCause();
                    assertThat(e.getResponse().getContentPath().toString(), is("servlet:///input.jsp"));
                    assertThat(e.getResponse().getStatusCode(), is(400));
                    assertThat(ae.getMessages().get(0).formatMessage(), is("メッセージ011"));
                    asserted.add(true);
                    throw e;
                }

            }
        })
        .addHandler(new Object() {
            @OnDoubleSubmission(path="/input.jsp", messageId="MSG00011")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse(200, "/complete.jsp");
            }
        }).startLocal();

        server.handle(request, null);
        assertEquals(1, asserted.size());
    }

    @Test
    public void testForDefaultWithInvalidTokenWithoutMessageId() {

        final List<Boolean> asserted = new ArrayList<Boolean>();

        HttpServer server = new HttpServer()
        .addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setUpInvalidToken(req, ctx);
                TokenTestUtil.setUpMessage(false);

                try {
                    HttpResponse response = ctx.handleNext(req);
                    fail();
                    return response;

                } catch (HttpErrorResponse e) {
                    assertThat(e.getResponse().getContentPath().toString(), is("servlet:///input.jsp"));
                    assertThat(e.getResponse().getStatusCode(), is(400));
                    assertNull(e.getCause());
                    asserted.add(true);
                    throw e;
                }
            }
        })
        .addHandler(new Object() {
            @OnDoubleSubmission(path="/input.jsp")
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse(200, "/complete.jsp");
            }
        })
        .startLocal();

        server.handle(request, null);
        assertEquals(1, asserted.size());
    }

    @Test
    public void testUsingDoubleSubmissionHandler() {

        final List<Boolean> asserted = new ArrayList<Boolean>();

        HttpServer server = new HttpServer()
        .addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setUpInvalidToken(req, ctx);
                TokenTestUtil.setUpMessage(true);

                try {
                    HttpResponse response = ctx.handleNext(req);
                    fail();
                    return response;

                } catch(HttpErrorResponse e) {
                    ApplicationException ae = (ApplicationException) e.getCause();
                    assertThat(e.getResponse().getContentPath().toString(), is("servlet:///common_error.jsp"));
                    assertThat(e.getResponse().getStatusCode(), is(200));
                    assertThat(ae.getMessages().get(0).formatMessage(), is("メッセージ022"));
                    asserted.add(true);
                    throw e;
                }
            }
        })
        .addHandler(new Object() {
            @OnDoubleSubmission()
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse(200, "/complete.jsp");
            }
        })
        .startLocal();

        server.handle(request, null);
        assertEquals(1, asserted.size());
    }

    @Test
    public void testUsingDoubleSubmissionHandlerWithSpecifyAnnotation() {

        final List<Boolean> asserted = new ArrayList<Boolean>();

        HttpServer server = new HttpServer()
        .addHandler(new Handler<HttpRequest, HttpResponse>() {
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                TokenTestUtil.setUpInvalidToken(request, ctx);
                TokenTestUtil.setUpMessage(true);
                try {
                    HttpResponse response = ctx.handleNext(req);
                    fail();
                    return response;
                } catch (HttpErrorResponse e) {
                    ApplicationException ae = (ApplicationException) e.getCause();
                    assertThat(e.getResponse().getContentPath().toString(), is("servlet:///common_error.jsp"));
                    assertThat(e.getResponse().getStatusCode(), is(500));
                    assertThat(ae.getMessages().get(0).formatMessage(), is("メッセージ022"));
                    asserted.add(true);
                    throw e;
                }
            }
        })
        .addHandler(new Object() {
            @OnDoubleSubmission(statusCode = 500)
            public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
                return new HttpResponse(200, "/complete.jsp");
            }
        })
        .startLocal();

        server.handle(request, null);
        assertEquals(1, asserted.size());
    }
}
