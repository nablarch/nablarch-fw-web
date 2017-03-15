package nablarch.fw.web.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

/**
 * 携帯対応ハンドラ {@link KeitaiAccessHandler} のテストケース
 */
public class KeitaiAccessHandlerTest {
    
    private KeitaiAccessHandler keitaiHandler = null;
    private HttpRequest request  = null;
    private HttpResponse response = null;
    private HttpRequest rewrittenRequest = null;
    private HttpResponse originalResponse = null;
    
    private Handler<HttpRequest, HttpResponse>
    testHandler = new Handler<HttpRequest, HttpResponse>() {
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            rewrittenRequest = req;
            return originalResponse;
        }
    };

    @Test
    public void testRewrite() {
        keitaiHandler = new KeitaiAccessHandler();
        
        request = new MockHttpRequest("GET /keitai/action/TestAction/keitaiMenu.do HTTP/1.1")
                .setParam(KeitaiAccessHandler.URI_OVERRIDE_PRAM_PREFIX
                        + "submitName|/keitai/action/rewritten/path/list.do", "");

        originalResponse = new HttpResponse("servlet:///keitai/pages/list.jsp");
        ExecutionContext context = new ExecutionContext()
                                      .addHandler(keitaiHandler)
                                      .addHandler(testHandler);
        
        response = context.handleNext(request);
        
        // リクエストの書き換え
        assertEquals("/keitai/action/rewritten/path/list.do", rewrittenRequest.getRequestPath());
        assertEquals("submitName", rewrittenRequest.getParam("nablarch_submit")[0]);
        assertTrue(context.getRequestScopeMap().containsKey(KeitaiAccessHandler.JS_UNSUPPORTED_FLAG_NAME));
        assertNotNull(context.getRequestScopedVar(KeitaiAccessHandler.JS_UNSUPPORTED_FLAG_NAME));
        
        assertEquals("servlet:///keitai/pages/list.jsp", response.getContentPath().toString());
    }
    
    /**
     * nablarch_uri_override_ ではじまるパラメータが存在しなければ
     * 書き換え処理は行われない。
     */
    @Test
    public void testRequestRewriteNotHappenWhenThereWasNoOverrideParameter() {
        keitaiHandler = new KeitaiAccessHandler();

        request = new MockHttpRequest("GET /keitai/action/TestAction/keitaiMenu.do HTTP/1.1");
        
        ExecutionContext context = new ExecutionContext()
                                      .addHandler(keitaiHandler)
                                      .addHandler(testHandler);

        originalResponse = new HttpResponse("servlet:///keitai/pages/list.jsp");
        response = context.handleNext(request);
        
        // リクエストの書き換えは行われない。
        assertEquals("/keitai/action/TestAction/keitaiMenu.do", rewrittenRequest.getRequestPath());
        assertNull(rewrittenRequest.getParam("nablarch_submit"));
        // しかしJS無効化フラグの設定は必ずやる。
        assertTrue(context.getRequestScopeMap().containsKey(KeitaiAccessHandler.JS_UNSUPPORTED_FLAG_NAME));
        assertNotNull(context.getRequestScopedVar(KeitaiAccessHandler.JS_UNSUPPORTED_FLAG_NAME));
        
    }
}
