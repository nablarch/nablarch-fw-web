package nablarch.fw.web.handler;

import nablarch.common.web.MockHttpSession;
import nablarch.common.web.handler.NormalHandler;
import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.SystemPropertyCleaner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link HttpAccessJsonLogFormatter}のテストクラス。
 *
 * @author Shuji Kitamura
 */
public class HttpAccessJsonLogFormatterTest extends LogTestSupport {
    @Rule
    public SystemPropertyCleaner systemPropertyCleaner = new SystemPropertyCleaner();

    @Before
    public void setup() {
        ThreadContext.clear();
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatBegin}メソッドのテスト。
     */
    @Test
    public void testFormatBegin() {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();

        MockServletRequest servletReq = extractMockServletRequest(logContext);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setMethod("POST");
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");

        ((MockHttpSession) servletReq.getSession()).setId("session_id_test");

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatBegin(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "HTTP ACCESS BEGIN")),
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
     * {@link HttpAccessJsonLogFormatter#formatBegin}メソッドのテスト。
     * <p>
     * {@code targets} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatBeginWithTarget() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label,url");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();

        MockServletRequest servletReq = extractMockServletRequest(logContext);
        servletReq.setRequestUrl("request_url_test");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatBegin(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("label", "HTTP ACCESS BEGIN")),
            withJsonPath("$", hasEntry("url", "request_url_test"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatBegin}メソッドのテスト。
     * <p>
     * {@code datePattern} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatBeginWithDatePattern() throws Exception {
        System.setProperty("httpAccessLogFormatter.beginTargets", "startTime,endTime");
        System.setProperty("httpAccessLogFormatter.datePattern", "yyyy/MM/dd HH:mm:ss");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setStartTime(toMilliseconds("2021-11-19 11:22:33.444"));
        logContext.setEndTime(toMilliseconds("2021-11-19 22:33:44.555"));

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatBegin(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("startTime", "2021/11/19 11:22:33")),
            withJsonPath("$", hasEntry("endTime", "2021/11/19 22:33:44"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatBegin}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testFormatBeginWithLabelValue() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        System.setProperty("httpAccessLogFormatter.beginLabel", "begin-label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatBegin(createEmptyLogContext());
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "begin-label"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatParameters}メソッドのテスト。
     */
    @Test
    public void testFormatParameters() {
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "req_param2");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        MockServletRequest servletReq = extractMockServletRequest(logContext);
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatParameters(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "PARAMETERS")),
                withJsonPath("$.parameters.req_param1", hasSize(1)),
                withJsonPath("$.parameters.req_param1[0]", equalTo("req_param1_test")),
                withJsonPath("$.parameters.req_param2", hasSize(1)),
                withJsonPath("$.parameters.req_param2[0]", equalTo("*****")),
                withJsonPath("$.parameters.req_param3", hasSize(1)),
                withJsonPath("$.parameters.req_param3[0]", equalTo("req_param3_test")))));
    }

    /**
     * マスキングの文字を指定できることをテスト。
     */
    @Test
    public void testMaskingChar() {
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "req_param2");
        System.setProperty("httpAccessLogFormatter.maskingChar", "@");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        MockServletRequest servletReq = extractMockServletRequest(logContext);
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatParameters(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "PARAMETERS")),
                withJsonPath("$.parameters.req_param1", hasSize(1)),
                withJsonPath("$.parameters.req_param1[0]", equalTo("req_param1_test")),
                withJsonPath("$.parameters.req_param2", hasSize(1)),
                withJsonPath("$.parameters.req_param2[0]", equalTo("@@@@@")),
                withJsonPath("$.parameters.req_param3", hasSize(1)),
                withJsonPath("$.parameters.req_param3[0]", equalTo("req_param3_test")))));
    }

    /**
     * formatBegin を無効化すると初期化が行われないことをテスト。
     */
    @Test
    public void testFormatBeginWhenDisabled() {
        // urlなどが含まれるとコンテキストのモック化をしないと出力が有効でもNPEが発生して
        // 判定が正しく行えなくなるので、出力を label だけに絞っている
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        System.setProperty("httpAccessLogFormatter.beginOutputEnabled", "false");

        final HttpAccessLogFormatter f = new HttpAccessJsonLogFormatter();
        final HttpAccessLogFormatter.HttpAccessLogContext context = createEmptyLogContext();

        // 初期化されていない状態でフォーマットを呼ぶと NPE が発生することを利用して確認する
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                f.formatBegin(context);
            }
        });
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatParameters}メソッドのテスト。
     * <p>
     * {@code targets} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatParametersWithTargets() {
        System.setProperty("httpAccessLogFormatter.parametersTargets", "label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatParameters(createEmptyLogContext());
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("label", "PARAMETERS"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatParameters(HttpAccessLogFormatter.HttpAccessLogContext)}メソッドのテスト。
     * <p>
     * {@code datePattern} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatParametersWithDatePattern() throws Exception {
        System.setProperty("httpAccessLogFormatter.parametersTargets", "startTime,endTime");
        System.setProperty("httpAccessLogFormatter.datePattern", "yyyy/MM/dd HH:mm:ss");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setStartTime(toMilliseconds("2021-11-19 11:22:33.444"));
        logContext.setEndTime(toMilliseconds("2021-11-19 22:33:44.555"));

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatParameters(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("startTime", "2021/11/19 11:22:33")),
            withJsonPath("$", hasEntry("endTime", "2021/11/19 22:33:44"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatParameters}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testFormatParametersWithLabelValue() {
        System.setProperty("httpAccessLogFormatter.parametersTargets", "label");
        System.setProperty("httpAccessLogFormatter.parametersLabel", "parameters-label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatParameters(createEmptyLogContext());
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "parameters-label"))
        )));
    }

    /**
     * formatParameters を無効化すると初期化が行われないことをテスト。
     */
    @Test
    public void testFormatParametersWhenDisabled() {
        System.setProperty("httpAccessLogFormatter.parametersOutputEnabled", "false");

        final HttpAccessLogFormatter f = new HttpAccessJsonLogFormatter();
        final HttpAccessLogFormatter.HttpAccessLogContext context = createEmptyLogContext();

        // 初期化されていない状態でフォーマットを呼ぶと NPE が発生することを利用して確認する
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                f.formatParameters(context);
            }
        });
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatDispatchingClass}メソッドのテスト。
     */
    @Test
    public void testFormatDispatchingClass() {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setDispatchingClass(NormalHandler.class.getName());

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatDispatchingClass(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "DISPATCHING CLASS")),
                withJsonPath("$", hasEntry("dispatchingClass", "nablarch.common.web.handler.NormalHandler")))));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatDispatchingClass}メソッドのテスト。
     * <p>
     * {@code targets} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatDispatchingClassWithTargets() {
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "dispatchingClass");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setDispatchingClass(NormalHandler.class.getName());

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatDispatchingClass(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("dispatchingClass", "nablarch.common.web.handler.NormalHandler"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatDispatchingClass(HttpAccessLogFormatter.HttpAccessLogContext)}メソッドのテスト。
     * <p>
     * {@code datePattern} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatDispatchingClassWithDatePattern() throws Exception {
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "startTime,endTime");
        System.setProperty("httpAccessLogFormatter.datePattern", "yyyy/MM/dd HH:mm:ss");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setStartTime(toMilliseconds("2021-11-19 11:22:33.444"));
        logContext.setEndTime(toMilliseconds("2021-11-19 22:33:44.555"));

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatDispatchingClass(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("startTime", "2021/11/19 11:22:33")),
            withJsonPath("$", hasEntry("endTime", "2021/11/19 22:33:44"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatDispatchingClass}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testFormatDispatchingClassWithLabelValue() {
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "label");
        System.setProperty("httpAccessLogFormatter.dispatchingClassLabel", "dispatching-label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatDispatchingClass(createEmptyLogContext());
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "dispatching-label"))
        )));
    }

    /**
     * formatDispatchingClass を無効化すると初期化が行われないことをテスト。
     */
    @Test
    public void testFormatDispatchingClassWhenDisabled() {
        System.setProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled", "false");

        final HttpAccessLogFormatter f = new HttpAccessJsonLogFormatter();
        final HttpAccessLogFormatter.HttpAccessLogContext context = createEmptyLogContext();

        // 初期化されていない状態でフォーマットを呼ぶと NPE が発生することを利用して確認する
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                f.formatDispatchingClass(context);
            }
        });
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     * <p>
     * {@code targets} 指定なしのデフォルト設定の場合。
     * </p>
     */
    @Test
    public void testFormatEnd() throws Exception {
        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        MockServletRequest servletReq = extractMockServletRequest(logContext);
        servletReq.setRequestUrl("request_url_test");
        logContext.setStartTime(toMilliseconds("2021-03-03 19:23:52.553"));
        logContext.setEndTime(toMilliseconds("2021-03-03 19:23:52.853"));
        logContext.setMaxMemory(2088763392);
        logContext.setFreeMemory(1088763392);

        ((MockHttpSession) servletReq.getSession()).setId("session_id_test");

        logContext.setResponse(new HttpResponse(404, "/success.jsp"));

        ThreadContext.setRequestId("request_id_test");
        ThreadContext.setUserId("user_id_test");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "HTTP ACCESS END")),
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
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     * <p>
     * {@code targets} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatEndWithTargets() {
        System.setProperty("httpAccessLogFormatter.endTargets", "queryString,queryString,sessionScope, ,responseStatusCode,clientUserAgent");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "sparam3");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        NablarchHttpServletRequestWrapper servletRequest = logContext.getContext().getServletRequest();
        servletRequest.getHeaderMap().put("User-Agent", "test user agent");

        logContext.setResponse(new HttpResponse(404, "/success.jsp"));

        Map<String, Object> sessionScope = logContext.getContext().getSessionScopeMap();
        sessionScope.put("sparam1", "sparam1_test");
        sessionScope.put("sparam2", "sparam2_test");
        sessionScope.put("sparam3", "sparam3_test");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("queryString", "")),
                withJsonPath("$.sessionScope", hasEntry("sparam1", "sparam1_test")),
                withJsonPath("$.sessionScope", hasEntry("sparam2", "sparam2_test")),
                withJsonPath("$.sessionScope", hasEntry("sparam3", "*****")),
                withJsonPath("$", hasEntry("responseStatusCode", 404)),
                withJsonPath("$", hasEntry("clientUserAgent", "test user agent")))));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd(HttpAccessLogFormatter.HttpAccessLogContext)}メソッドのテスト。
     * <p>
     * {@code datePattern} 指定ありの場合。
     * </p>
     */
    @Test
    public void testFormatEndWithoutDatePattern() throws Exception {
        System.setProperty("httpAccessLogFormatter.endTargets", "startTime,endTime");
        System.setProperty("httpAccessLogFormatter.datePattern", "yyyy/MM/dd HH:mm:ss");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setStartTime(toMilliseconds("2021-11-19 11:22:33.444"));
        logContext.setEndTime(toMilliseconds("2021-11-19 22:33:44.555"));

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("startTime", "2021/11/19 11:22:33")),
            withJsonPath("$", hasEntry("endTime", "2021/11/19 22:33:44"))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     * <p>
     * {@link HttpAccessLogFormatter.HttpAccessLogContext} に {@link HttpResponse} が設定されていない場合。
     * </p>
     */
    @Test
    public void testFormatEndWithoutStatusCode() {
        System.setProperty("httpAccessLogFormatter.endTargets", "statusCode,responseStatusCode");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "sparam3");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withoutJsonPath("$.statusCode"),
            withJsonPath("$", hasEntry("responseStatusCode", 200))
        )));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     * <p>
     * クライアントに返すステータスコードが -1 の場合。
     * </p>
     */
    @Test
    public void testFormatEndStatusCodeIsMinusOne() {
        System.setProperty("httpAccessLogFormatter.endTargets", "responseStatusCode");

        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();
        logContext.setResponse(new HttpResponse() {
            @Override
            public int getStatusCode() {
                return -1;
            }
        });

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(logContext);
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(withoutJsonPath("$.responseStatusCode")));
    }

    /**
     * 不正なターゲットのテスト。
     */
    @Test
    public void testFormatEndWithIllegalTargets() {
        System.setProperty("httpAccessLogFormatter.endTargets", "queryString,dummy,responseStatusCode,clientUserAgent");

        Exception e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
            }
        });

        assertThat(e.getMessage(), is("[dummy] is unknown target. property name = [httpAccessLogFormatter.endTargets]"));
    }

    /**
     * {@link HttpAccessJsonLogFormatter#formatEnd}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testFormatEndWithLabelValue() {
        System.setProperty("httpAccessLogFormatter.endTargets", "label");
        System.setProperty("httpAccessLogFormatter.endLabel", "end-label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatEnd(createEmptyLogContext());
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "end-label"))
        )));
    }

    /**
     * formatEnd を無効化すると初期化が行われないことをテスト。
     */
    @Test
    public void testFormatEndWhenDisabled() {
        // urlなどが含まれるとコンテキストのモック化をしないと出力が有効でもNPEが発生して
        // 判定が正しく行えなくなるので、出力を label だけに絞っている
        System.setProperty("httpAccessLogFormatter.endTargets", "label");
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "false");

        final HttpAccessLogFormatter f = new HttpAccessJsonLogFormatter();
        final HttpAccessLogFormatter.HttpAccessLogContext context = createEmptyLogContext();

        // 初期化されていない状態でフォーマットを呼ぶと NPE が発生することを利用して確認する
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                f.formatEnd(context);
            }
        });
    }

    /**
     * formatEnd のターゲットにメモリ関係の項目が指定されていない場合
     * containsMemoryItemはfalseとなることをテスト。
     */
    @Test
    public void testContainsMemoryItem_false_ifNoMemoryTargetsInFormatEnd() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "maxMemory,freeMemory");
        System.setProperty("httpAccessLogFormatter.parametersTargets", "maxMemory,freeMemory");
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "maxMemory,freeMemory");
        System.setProperty("httpAccessLogFormatter.endTargets", "label");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        assertThat(formatter.containsMemoryItem(), is(false));
    }

    /**
     * formatEnd のターゲットに maxMemory が設定されている場合は、
     * containsMemoryItem に true が設定されていることをテスト。
     */
    @Test
    public void testContainsMemoryItem_true_ifMaxMemoryExistsInFormatEnd() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        System.setProperty("httpAccessLogFormatter.parametersTargets", "label");
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "label");
        System.setProperty("httpAccessLogFormatter.endTargets", "label,maxMemory");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        assertThat(formatter.containsMemoryItem(), is(true));
    }

    /**
     * formatEnd のターゲットに freeMemory が設定されている場合は、
     * containsMemoryItem に true が設定されていることをテスト。
     */
    @Test
    public void testContainsMemoryItem_true_ifFreeMemoryExistsInFormatEnd() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        System.setProperty("httpAccessLogFormatter.parametersTargets", "label");
        System.setProperty("httpAccessLogFormatter.dispatchingClassTargets", "label");
        System.setProperty("httpAccessLogFormatter.endTargets", "label,freeMemory");

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();

        assertThat(formatter.containsMemoryItem(), is(true));
    }

    /**
     * {@code structuredMessagePrefix}の指定ができることをテスト。
     */
    @Test
    public void testStructuredMessagePrefix() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        System.setProperty("httpAccessLogFormatter.structuredMessagePrefix", "@JSON@");
        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter();
        String message = formatter.formatBegin(logContext);
        assertThat(message.startsWith("@JSON@"), is(true));
        assertThat(message.substring("@JSON@".length()), isJson(allOf(
            withJsonPath("$", hasEntry("label", "HTTP ACCESS BEGIN"))
        )));
    }

    /**
     * {@code jsonSerializationManagerClassName}の指定ができることをテスト。
     */
    @Test
    public void testJsonSerializationManagerClassName() {
        System.setProperty("httpAccessLogFormatter.beginTargets", "label");
        HttpAccessLogFormatter.HttpAccessLogContext logContext = createEmptyLogContext();

        HttpAccessLogFormatter formatter = new HttpAccessJsonLogFormatter() {
            @Override
            protected JsonSerializationManager createSerializationManager(JsonSerializationSettings settings) {
                assertThat(settings.getProp("beginTargets"), is("label"));
                return new MockJsonSerializationManager();
            }
        };
        String message = formatter.formatBegin(logContext);

        assertThat(message, is("$JSON$mock serialization"));
    }

    public static class MockJsonSerializationManager extends BasicJsonSerializationManager {

        @Override
        public JsonSerializer getSerializer(Object value) {
            return new JsonSerializer() {
                @Override
                public void serialize(Writer writer, Object value) throws IOException {
                    writer.write("mock serialization");
                }

                @Override
                public void initialize(JsonSerializationSettings settings) {
                }

                @Override
                public boolean isTarget(Class<?> valueClass) {
                    return true;
                }
            };
        }
    }

    /**
     * テスト用に、個々の属性が設定されていない空のログコンテキストを生成する。
     * @return 空のログコンテキスト
     */
    private HttpAccessLogFormatter.HttpAccessLogContext createEmptyLogContext() {
        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setSession(new MockHttpSession());
        ServletExecutionContext servletExecutionContext = new ServletExecutionContext(servletReq, null, null);

        HttpAccessLogFormatter.HttpAccessLogContext logContext = new HttpAccessLogFormatter.HttpAccessLogContext();
        logContext.setContext(servletExecutionContext);
        logContext.setRequest(servletExecutionContext.getHttpRequest());

        return logContext;
    }

    /**
     * テスト用に生成されたログコンテキストから、モックのリクエストオブジェクトを抽出する。
     * @param logContext {@link #createEmptyLogContext()} で生成されたモックのログコンテキスト
     * @return ログコンテキストから抽出したモックのリクエストオブジェクト
     */
    private MockServletRequest extractMockServletRequest(HttpAccessLogFormatter.HttpAccessLogContext logContext) {
        return ((MockServletRequest) logContext.getContext().getServletRequest().getRequest());
    }

    /**
     * 日付文字列をミリ秒に変換する。
     * @param textDate 日付文字列({@code "yyyy-MM-dd HH:mm:ss.SSS"} 形式)
     * @return 日付をミリ秒に変換した結果
     * @throws Exception 日付文字列のパースに失敗した場合
     */
    private long toMilliseconds(String textDate) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(textDate).getTime();
    }
}
