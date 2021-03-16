package nablarch.fw.web.handler;

import nablarch.common.web.MockHttpSession;
import nablarch.common.web.handler.NormalHandler;
import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link HttpAccessJsonLogFormatter}のテストクラス。
 *
 * @author Shuji Kitamura
 */
public class HttpAccessJsonLogFormatterTest extends LogTestSupport {

    @Before
    public void setup() {
        System.clearProperty("httpAccessLogFormatter.beginOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.parametersOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.endOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.endTargets");
        System.clearProperty("httpAccessLogFormatter.maskingPatterns");
        ThreadContext.clear();
    }

    @After
    public void teardown() {
        System.clearProperty("httpAccessLogFormatter.beginOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.parametersOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.endOutputEnabled");
        System.clearProperty("httpAccessLogFormatter.endTargets");
        System.clearProperty("httpAccessLogFormatter.maskingPatterns");
        ThreadContext.clear();
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatBegin}メソッドのテスト。
     */
    @Test
    public void testFormatBegin() {
//        System.setProperty("httpAccessLogFormatter.parametersOutputEnabled", "false");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContext();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatBegin(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("requestId", "request_id_test")),
                withJsonPath("$", hasEntry("userId", "user_id_test")),
                withJsonPath("$", hasEntry("sessionId", "session_id_test")),
                withJsonPath("$", hasEntry("url", "request_url_test")),
                withJsonPath("$", hasEntry("method", "POST")),
                withJsonPath("$", hasEntry("port", 9999)),
                withJsonPath("$", hasEntry("clientIpAddress", "remote_addr_test")),
                withJsonPath("$", hasEntry("clientHost", "remote_host_test")))));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatParameters}メソッドのテスト。
     */
    @Test
    public void testFormatParameters() {
        System.setProperty("httpAccessLogFormatter.beginOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "req_param2");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContext();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatParameters(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$.parameters.req_param1", hasSize(1)),
                withJsonPath("$.parameters.req_param1[0]", equalTo("req_param1_test")),
                withJsonPath("$.parameters.req_param2", hasSize(1)),
                withJsonPath("$.parameters.req_param2[0]", equalTo("*****")),
                withJsonPath("$.parameters.req_param3", hasSize(1)),
                withJsonPath("$.parameters.req_param3[0]", equalTo("req_param3_test")))));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatDispatchingClass}メソッドのテスト。
     */
    @Test
    public void testFormatDispatchingClass() {
        System.setProperty("httpAccessLogFormatter.beginOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.parametersOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "false");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContext();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatDispatchingClass(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("dispatchingClass", "nablarch.common.web.handler.NormalHandler")))));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     */
    @Test
    public void testFormatEnd() {
        System.setProperty("httpAccessLogFormatter.beginOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.parametersOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled", "false");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContext();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("requestId", "request_id_test")),
                withJsonPath("$", hasEntry("userId", "user_id_test")),
                withJsonPath("$", hasEntry("sessionId", "session_id_test")),
                withJsonPath("$", hasEntry("url", "request_url_test")),
                withJsonPath("$", hasEntry("statusCode", 404)),
                withJsonPath("$", hasEntry("contentPath", "servlet:///success.jsp")),
                withJsonPath("$", hasEntry("startTime", "2021-03-03 19:23:52.553")),
                withJsonPath("$", hasEntry("endTime", "2021-03-03 19:23:52.853")),
                withJsonPath("$", hasEntry("executionTime", 300)),
                withJsonPath("$", hasEntry("maxMemory", 2088763392)),
                withJsonPath("$", hasEntry("freeMemory", 1088763392)))));

        assertThat(formatter.containsMemoryItem(), is(true));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     */
    @Test
    public void testFormatEndWithTargets() {
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "true");
        System.setProperty("httpAccessLogFormatter.endTargets", "queryString,queryString,sessionScope, ,responseStatusCode,clientUserAgent");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "sparam3");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContext();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("queryString", "")),
                withJsonPath("$.sessionScope", hasEntry("sparam1", "sparam1_test")),
                withJsonPath("$.sessionScope", hasEntry("sparam2", "sparam2_test")),
                withJsonPath("$.sessionScope", hasEntry("sparam3", "*****")),
                withJsonPath("$", hasEntry("responseStatusCode", 404)),
                withJsonPath("$", hasEntry("clientUserAgent", "test user agent")))));

        assertThat(formatter.containsMemoryItem(), is(false));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     */
    @Test
    public void testFormatEndWithNoStatusCode() {
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "true");
        System.setProperty("httpAccessLogFormatter.endTargets", "statusCode,responseStatusCode");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "sparam3");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createLogContextNoRes();

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("responseStatusCode", 200)))));

        assertThat(formatter.containsMemoryItem(), is(false));
    }

    /**
     * 不正なターゲットのテスト。
     */
    @Test
    public void testFormatEndWithIllegalTargets() {
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "true");
        System.setProperty("httpAccessLogFormatter.endTargets", "queryString,dummy,responseStatusCode,clientUserAgent");

        Exception e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
            }
        });

        assertThat(e.getMessage(), is("[dummy] is unknown target. property name = [httpAccessLogFormatter.endTargets]"));
    }


    private HttpAccessLogFormatter.HttpAccessLogContext createLogContext() {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = new HttpAccessLogFormatter.HttpAccessLogContext();

        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");

        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("");
        servletReq.setSession(session);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setRequestURI("/success.jsp");
        servletReq.setMethod("POST");
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});
        servletReq.addHeader("User-Agent", "test user agent");

        ServletExecutionContext servletExecutionContext = new ServletExecutionContext(servletReq, null, null);
        logContext.setContext(servletExecutionContext);
        logContext.setRequest(servletExecutionContext.getHttpRequest());

        Object aaa = logContext.getParameters();
        Object bbb = logContext.getServletRequest();

        Map<String, Object> sessionScope = servletExecutionContext.getSessionScopeMap();
        sessionScope.put("sparam1", "sparam1_test");
        sessionScope.put("sparam2", "sparam2_test");
        sessionScope.put("sparam3", "sparam3_test");

        logContext.setDispatchingClass(NormalHandler.class.getName());

        HttpResponse response = new HttpResponse(404, "/success.jsp");
        logContext.setResponse(response);

        logContext.setStartTime(1614767032553l);
        logContext.setEndTime(1614767032853l);
        logContext.setMaxMemory(2088763392);
        logContext.setFreeMemory(1088763392);

        return logContext;
    }

    private HttpAccessLogFormatter.HttpAccessLogContext createLogContextNoRes() {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = new HttpAccessLogFormatter.HttpAccessLogContext();

        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");

        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("");
        servletReq.setSession(session);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setRequestURI("/success.jsp");
        servletReq.setMethod("POST");
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});
        servletReq.addHeader("User-Agent", "test user agent");

        ServletExecutionContext servletExecutionContext = new ServletExecutionContext(servletReq, null, null);
        logContext.setContext(servletExecutionContext);
        logContext.setRequest(servletExecutionContext.getHttpRequest());

        Object aaa = logContext.getParameters();
        Object bbb = logContext.getServletRequest();

        Map<String, Object> sessionScope = servletExecutionContext.getSessionScopeMap();
        sessionScope.put("sparam1", "sparam1_test");
        sessionScope.put("sparam2", "sparam2_test");
        sessionScope.put("sparam3", "sparam3_test");

        logContext.setDispatchingClass(NormalHandler.class.getName());

        logContext.setStartTime(1614767032553l);
        logContext.setEndTime(1614767032853l);
        logContext.setMaxMemory(2088763392);
        logContext.setFreeMemory(1088763392);

        return logContext;
    }

    private HttpAccessLogFormatter.HttpAccessLogContext createLogContextNoResStCd() {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = new HttpAccessLogFormatter.HttpAccessLogContext();

        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");

        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("");
        servletReq.setSession(session);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setRequestURI("/success.jsp");
        servletReq.setMethod("POST");
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});
        servletReq.addHeader("User-Agent", "test user agent");
        servletReq.addHeader("X-Requested-With", "");

        ServletExecutionContext servletExecutionContext = new ServletExecutionContext(servletReq, null, null);
        logContext.setContext(servletExecutionContext);
        logContext.setRequest(servletExecutionContext.getHttpRequest());

        Object aaa = logContext.getParameters();
        Object bbb = logContext.getServletRequest();

        Map<String, Object> sessionScope = servletExecutionContext.getSessionScopeMap();
        sessionScope.put("sparam1", "sparam1_test");
        sessionScope.put("sparam2", "sparam2_test");
        sessionScope.put("sparam3", "sparam3_test");

        logContext.setDispatchingClass(NormalHandler.class.getName());

        HttpResponse response = new HttpResponse(-1, "/success.jsp");
        logContext.setResponse(response);

        logContext.setStartTime(1614767032553l);
        logContext.setEndTime(1614767032853l);
        logContext.setMaxMemory(2088763392);
        logContext.setFreeMemory(1088763392);

        return logContext;
    }


}
