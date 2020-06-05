package nablarch.fw.web.handler;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nablarch.common.web.MockHttpSession;
import nablarch.common.web.handler.NormalHandler;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogTestUtil;
import nablarch.core.log.Logger;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpAccessLogFormatter.HttpAccessLogContext;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Test;

public class HttpAccessLogFormatterTest extends LogTestSupport {
    
    private HttpAccessLogContext logContext;
    
    private void init() {
        logContext = new HttpAccessLogContext();
        HttpServletRequest servletReq = initServletReq("POST", "/handler/NormalHandler/index.html");
        ServletExecutionContext context = new ServletExecutionContext(servletReq, null, null);
        logContext.setContext(context);
        logContext.setRequest(context.getHttpRequest());
        Map<String, Object> sessionScope = context.getSessionScopeMap();
        sessionScope.put("sparam1", "sparam1_test");
        sessionScope.put("sparam2", "sparam2_test");
        sessionScope.put("sparam3", "sparam3_test");
        logContext.setDispatchingClass(NormalHandler.class.getName());
        HttpResponse response = new HttpResponse(404, "/success.jsp");
        logContext.setResponse(response);
    }
    private MockServletRequest initServletReq(String method, String uri) {
        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");
        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("");
        servletReq.setSession(session);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setRequestURI(uri);
        servletReq.setMethod(method);
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");
        servletReq.getParams().put("req_param1", new String[] {"req_param1_test"});
        servletReq.getParams().put("req_param2", new String[] {"req_param2_test"});
        servletReq.getParams().put("req_param3", new String[] {"req_param3_test"});
        servletReq.addHeader("User-Agent", "test user agent");
        return servletReq;
    }

    private void initWithGetRequest() {
        logContext = new HttpAccessLogContext();
        MockHttpSession session = new MockHttpSession();
        session.setId("session_id_test");
        MockServletRequest servletReq = new MockServletRequest();
        servletReq.setContextPath("");
        servletReq.setSession(session);
        servletReq.setRequestUrl("request_url_test");
        servletReq.setQueryString("req_param2=req_param2_test");
        servletReq.setMethod("GET");
        servletReq.setServerPort(9999);
        servletReq.setRemoteAddr("remote_addr_test");
        servletReq.setRemoteHost("remote_host_test");
        servletReq.addHeader("User-Agent", "test user agent");

        ServletExecutionContext context = new ServletExecutionContext(servletReq, null, null);
        logContext.setContext(context);
        logContext.setRequest(context.getHttpRequest());
        logContext.setDispatchingClass(NormalHandler.class.getName());
        HttpResponse response = new HttpResponse(404, "/success.jsp");
        logContext.setResponse(response);
    }

    /**
     * Sessionが存在しない場合にSessionを作成しないことの確認。
     */
    @Test
    public void testNoSession() {
        System.setProperty("httpAccessLogFormatter.beginFormat",
                "sessionScope = [$sessionScope$]"
              + "\n\tparameters  = [$parameters$]"
              + "\n\t> sid = [$sessionId$]" 
              + " @@@@ BEGIN @@@@");
        /** session error */
        MockServletRequest request = new MockServletRequest(){
            public HttpSession getSession() {
                fail("セッションを生成してはいけない");
                return null; // for compile.
            }
            public HttpSession getSession(boolean create) {
                assertFalse("セッションを生成してはいけない。", create);
                return null;
            }
        };
        ServletExecutionContext context = new ServletExecutionContext(request, null, null);
        HttpAccessLogContext logContext = new HttpAccessLogContext();
        logContext.setContext(context);
        logContext.setRequest(context.getHttpRequest());
        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();
        String message = formatter.formatBegin(logContext);
        String[] splitMsg = message.split(Logger.LS);
        int index = 0;
        assertThat(splitMsg[index++], is("sessionScope = [{}]"));
        assertThat(splitMsg[index++], is("\tparameters  = [{}]"));
        assertThat(splitMsg[index++], is("\t> sid = [] @@@@ BEGIN @@@@"));
    }

    /**
     * クエリ文字列が正しく出力されること。
     */
    @Test
    public void testQueryString() {

        initWithGetRequest();

        System.setProperty("httpAccessLogFormatter.beginFormat",
                          "\n\tmethod      = [$method$]"
                        + "\n\turl         = [$url$$query$]"
                        + "\n @@@@ BEGIN @@@@");

        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();

        String message = formatter.formatBegin(logContext);
        String[] splitMsg = message.split(Logger.LS);
        int index = 0;

        assertThat(splitMsg[index++], is("method      = [GET]"));
        assertThat(splitMsg[index++], is("\turl         = [request_url_test?req_param2=req_param2_test]"));
    }
    /**
     * フォーマットの出力項目を入れ替えた場合に正しくフォーマットされること。
     */
    @Test
    public void testFormatSettingsSwap() {
        
        init();
        
        System.setProperty("httpAccessLogFormatter.beginFormat",
                "sessionScope = [$sessionScope$]"
              + "\n\tparameters  = [$parameters$]"
              + "\n\tclient_host = [$clientHost$]"
              + "\n\tclient_ip   = [$clientIpAddress$]"
              + "\n\tport        = [$port$]"
              + "\n\tmethod      = [$method$]"
              + "\n\turl         = [$url$]"
              + "\n\tquery       = [$query$]"
              + "\n\tuser_agent  = [$clientUserAgent$]" 
              + "\n\t> sid = [$sessionId$]" 
              + " @@@@ BEGIN @@@@");
        System.setProperty("httpAccessLogFormatter.parametersFormat",
                "parameters  = [$parameters$]"
              + "\n\t> sid = [$sessionId$] @@@@ PARAMETERS @@@@");
        System.setProperty("httpAccessLogFormatter.dispatchingClassFormat",
                "class = [$dispatchingClass$]"
              + "\n\t> sid = [$sessionId$] @@@@ DISPATCHING CLASS @@@@");
        System.setProperty("httpAccessLogFormatter.endFormat",
                "max_memory = [$maxMemory$] free_memory = [$freeMemory$]"
              + " start_time = [$startTime$] end_time = [$endTime$] execution_time = [$executionTime$]"
              + " < sid = [$sessionId$] @@@@ END @@@@ url = [$url$] status_code = [$statusCode$] content_path = [$contentPath$]"
              + " response_status_code = [$responseStatusCode$]");
        System.setProperty("httpAccessLogFormatter.datePattern", "yyyy/MM/dd HH-mm-ss[SSS]");
        
        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();
        
        String message = formatter.formatBegin(logContext);
        String[] splitMsg = message.split(Logger.LS);
        int index = 0;
        
        assertThat(splitMsg[index++], is("sessionScope = [{"));
        assertTrue(Pattern.compile("\t\tsparam[1-3] = \\[sparam[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\tsparam[1-3] = \\[sparam[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\tsparam[1-3] = \\[sparam[1-3]_test\\]}]").matcher(splitMsg[index++]).matches());
        assertThat(splitMsg[index++], is("\tparameters  = [{"));
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\]}]").matcher(splitMsg[index++]).matches());
        assertThat(splitMsg[index++], is("\tclient_host = [remote_host_test]"));
        assertThat(splitMsg[index++], is("\tclient_ip   = [remote_addr_test]"));
        assertThat(splitMsg[index++], is("\tport        = [9999]"));
        assertThat(splitMsg[index++], is("\tmethod      = [POST]"));
        assertThat(splitMsg[index++], is("\turl         = [request_url_test]"));
        assertThat(splitMsg[index++], is("\tquery       = []"));
        assertThat(splitMsg[index++], is("\tuser_agent  = [test user agent]"));
        assertThat(splitMsg[index++], is("\t> sid = [session_id_test] @@@@ BEGIN @@@@"));
        
        message = formatter.formatParameters(logContext);
        splitMsg = message.split(Logger.LS);
        index = 0;
        
        assertThat(splitMsg[index++], is("parameters  = [{"));
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\],").matcher(splitMsg[index++]).matches());
        assertTrue(Pattern.compile("\t\treq_param[1-3] = \\[req_param[1-3]_test\\]}]").matcher(splitMsg[index++]).matches());
        assertThat(splitMsg[index++], is("\t> sid = [session_id_test] @@@@ PARAMETERS @@@@"));
        
        message = formatter.formatDispatchingClass(logContext);
        splitMsg = message.split(Logger.LS);
        index = 0;
        
        assertThat(splitMsg[index++], is("class = [" + NormalHandler.class.getName() + "]"));
        assertThat(splitMsg[index++], is("\t> sid = [session_id_test] @@@@ DISPATCHING CLASS @@@@"));
        
        message = formatter.formatEnd(logContext);
        splitMsg = message.split(Logger.LS);
        index = 0;
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH-mm-ss[SSS]");
        Pattern pattern = Pattern.compile("^\\[(.+)\\]");
        
        String[] resHeader = splitMsg[index++].split(" ");
        int headerIndex = 0;
        
        assertThat(resHeader[headerIndex++], is("max_memory"));
        assertThat(resHeader[headerIndex++], is("="));
        assertTrue(Pattern.compile("\\[[0-9]+\\]").matcher(resHeader[headerIndex++]).matches());
        assertThat(resHeader[headerIndex++], is("free_memory"));
        assertThat(resHeader[headerIndex++], is("="));
        assertTrue(Pattern.compile("\\[[0-9]+\\]").matcher(resHeader[headerIndex++]).matches());
        assertThat(resHeader[headerIndex++], is("start_time"));
        assertThat(resHeader[headerIndex++], is("="));
        assertNotNull(LogTestUtil.parseDate(resHeader[headerIndex++] + " " + resHeader[headerIndex++], dateFormat, pattern));
        assertThat(resHeader[headerIndex++], is("end_time"));
        assertThat(resHeader[headerIndex++], is("="));
        assertNotNull(LogTestUtil.parseDate(resHeader[headerIndex++] + " " + resHeader[headerIndex++], dateFormat, pattern));
        assertThat(resHeader[headerIndex++], is("execution_time"));
        assertThat(resHeader[headerIndex++], is("="));
        assertTrue(Pattern.compile("\\[[0-9]+\\]").matcher(resHeader[headerIndex++]).matches());
        assertThat(resHeader[headerIndex++], is("<"));
        assertThat(resHeader[headerIndex++], is("sid"));
        assertThat(resHeader[headerIndex++], is("="));
        assertThat(resHeader[headerIndex++], is("[session_id_test]"));
        assertThat(resHeader[headerIndex++], is("@@@@"));
        assertThat(resHeader[headerIndex++], is("END"));
        assertThat(resHeader[headerIndex++], is("@@@@"));
        assertThat(resHeader[headerIndex++], is("url"));
        assertThat(resHeader[headerIndex++], is("="));
        assertThat(resHeader[headerIndex++], is("[request_url_test]"));
        assertThat(resHeader[headerIndex++], is("status_code"));
        assertThat(resHeader[headerIndex++], is("="));
        assertThat(resHeader[headerIndex++], is("[404]"));
        assertThat(resHeader[headerIndex++], is("content_path"));
        assertThat(resHeader[headerIndex++], is("="));
        assertThat(resHeader[headerIndex++], is("[servlet:///success.jsp]"));
        assertThat(resHeader[headerIndex++], is("response_status_code"));
        assertThat(resHeader[headerIndex++], is("="));
        assertThat(resHeader[headerIndex++], is("[404]"));
    }
    
    /**
     * マスキングが指定された場合に、正しくフォーマットできること。
     */
    @Test
    public void testMaskingSettings() {
        
        init();
        
        System.setProperty("httpAccessLogFormatter.beginFormat",
                           "[$sessionId$] @@@@ BEGIN @@@@\n\tparameters  = [$parameters$]");
        System.setProperty("httpAccessLogFormatter.maskingPatterns", "xxx,.*param2, ,.*param3,zzz");
        System.setProperty("httpAccessLogFormatter.maskingChar", "a");
        
        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();
        
        String message = formatter.formatBegin(logContext);
        String[] splitMsg = message.split(Logger.LS);
        int index = 0;
        
        assertThat(splitMsg[index++], is("[session_id_test] @@@@ BEGIN @@@@"));
        assertThat(splitMsg[index++], is("\tparameters  = [{"));
        int baseIndex = index++;
        for (int i = baseIndex; i < baseIndex + 3; i++) {
            if (splitMsg[i].contains("param1")) {
                assertTrue(Pattern.compile("\t\treq_param1 = \\[req_param1_test\\].+").matcher(splitMsg[i]).matches());
            } else {
                assertTrue(Pattern.compile("\t\treq_param[23] = \\[aaaaa\\].+").matcher(splitMsg[i]).matches());
            }
        }
    }

    /**
     * マスキング文字が2文字以上の場合に例外が送出されること。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMaskingIllegalSettings() {
        System.setProperty("httpAccessLogFormatter.maskingChar", "aa");
        new HttpAccessLogFormatter();
    }

    /**
     * マスキング文字が0文字の場合に例外が送出されること。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMaskingZeroIllegalSettings() {
        System.setProperty("httpAccessLogFormatter.maskingChar", "");
        new HttpAccessLogFormatter();
    }

    /**
     * リクエストパラメータとセッション情報のセパレータが指定された場合に、正しくフォーマットできること。
     */
    @Test
    public void testParamsSeparatorSettings() {
        
        init();
        
        System.setProperty("httpAccessLogFormatter.beginFormat",
                           "[$sessionId$] @@@@ BEGIN @@@@\n\tparameters = [$parameters$]\n\tsessionScope = [$sessionScope$]");
        System.setProperty("httpAccessLogFormatter.parametersSeparator", "@");
        System.setProperty("httpAccessLogFormatter.sessionScopeSeparator", "%");
        
        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();
        
        String message = formatter.formatBegin(logContext);
        String[] splitMsg = message.split(Logger.LS);
        int index = 0;
        
        assertThat(splitMsg[index++], is("[session_id_test] @@@@ BEGIN @@@@"));
        
        String[] paramsItem = splitMsg[index++].split("@");
        int paramsIndex = 0;
        assertTrue(Pattern.compile(".*req_param[1-3] = \\[req_param[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
        assertTrue(Pattern.compile(".*req_param[1-3] = \\[req_param[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
        assertTrue(Pattern.compile(".*req_param[1-3] = \\[req_param[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
        
        paramsItem = splitMsg[index++].split("%");
        paramsIndex = 0;
        assertTrue(Pattern.compile(".*sparam[1-3] = \\[sparam[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
        assertTrue(Pattern.compile(".*sparam[1-3] = \\[sparam[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
        assertTrue(Pattern.compile(".*sparam[1-3] = \\[sparam[1-3]_test\\].+").matcher(paramsItem[paramsIndex++]).matches());
    }
    
    /**
     * 出力タイミング毎の出力可否を設定できること。
     */
    @Test
    public void testOutputEnabledSettings() {
        System.setProperty("httpAccessLogFormatter.beginOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.parametersOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.dispatchingClassOutputEnabled", "false");
        System.setProperty("httpAccessLogFormatter.endOutputEnabled", "false");
        
        HttpAccessLogFormatter formatter = new HttpAccessLogFormatter();
        assertFalse(formatter.isBeginOutputEnabled());
        assertFalse(formatter.isParametersOutputEnabled());
        assertFalse(formatter.isDispatchingClassOutputEnabled());
        assertFalse(formatter.isEndOutputEnabled());
    }
}
