package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.Test;

/**
 * 
 * {@link HttpResponseHandlerTest}テスト。
 * 
 * @author Koichi Asano
 *
 */
public class HttpResponseUtilTest {
    /**
     * Ajaxの場合と、HTTPレスポンスコードの変換モードがCONVERT_ALL_TO_200の場合のテスト。<br />
     */
    @Test
    public void testChooseResponseStatusCodeForConvertAllTo200() {
        {
            // AJAX リクエストの場合
            MockServletRequest req = new MockServletRequest();
            req.setContextPath("");
            req.addHeader("X-Requested-With", "XMLHttpRequest");
            req.setRequestUrl("request_url_test");

            ServletExecutionContext ctx = new ServletExecutionContext(req,
                    null, null);
            HttpResponseUtil.setStatusConvertMode(ctx, HttpResponseUtil.StatusConvertMode.CONVERT_ALL_TO_200);
            HttpResponse res = new HttpResponse(400);


            assertThat(HttpResponseUtil.chooseResponseStatusCode(res, ctx),
                    is(400));

        }
        {
            MockServletRequest req = new MockServletRequest();
            req.setContextPath("");
            req.setRequestUrl("request_url_test");

            ServletExecutionContext ctx = new ServletExecutionContext(req,
                    null, null);
            HttpResponseUtil.setStatusConvertMode(ctx, HttpResponseUtil.StatusConvertMode.CONVERT_ALL_TO_200);

            assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(302), ctx),
                    is(302));


            assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(200), ctx),
                    is(200));

            assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(400), ctx),
                    is(200));

            assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(404), ctx),
                    is(200));

            assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(503), ctx),
                    is(200));

            assertThat(HttpResponseUtil.chooseResponseStatusCode(null, ctx),
                    is(200));
        }
    }

    /**
     * HTTPレスポンスコードの変換モードがデフォルト(null)の場合のテスト<br />
     */
    @Test
    public void testChooseResponseStatusCodeForConvertDefault() {

        MockServletRequest req = new MockServletRequest();
        req.setContextPath("");
        req.setRequestUrl("request_url_test");

        ServletExecutionContext ctx = new ServletExecutionContext(req, null,null);
        HttpResponseUtil.setStatusConvertMode(ctx, null);

        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(302), ctx), is(302));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(200), ctx), is(200));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(201), ctx), is(201));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(400), ctx), is(200));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(404), ctx), is(404));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(503), ctx), is(503));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(null, ctx),is(200));
    }

    /**
     * HTTPレスポンスコードの変換モードがCONVERT_ONLY_400_TO_200の場合のテスト<br />
     */
    @Test
    public void testChooseResponseStatusCodeForConvertOnly400To200() {

        MockServletRequest req = new MockServletRequest();
        req.setContextPath("");
        req.setRequestUrl("request_url_test");

        ServletExecutionContext ctx = new ServletExecutionContext(req, null,null);
        HttpResponseUtil.setStatusConvertMode(ctx, HttpResponseUtil.StatusConvertMode.CONVERT_ONLY_400_TO_200);

        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(302), ctx), is(302));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(200), ctx), is(200));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(201), ctx), is(201));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(400), ctx), is(200));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(404), ctx), is(404));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(new HttpResponse(503), ctx), is(503));
        assertThat(HttpResponseUtil.chooseResponseStatusCode(null, ctx),is(200));
    }
}
