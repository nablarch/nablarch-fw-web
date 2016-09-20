package nablarch.fw.web.upload;


import nablarch.fw.ExecutionContext;
import nablarch.fw.handler.RequestPathJavaPackageMapping;
import nablarch.fw.results.InternalError;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.ForwardingHandler;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.MockServletInputStream;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static nablarch.fw.web.upload.UploadTestUtil.readAll;
import static nablarch.test.Assertion.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link MultipartHandler}のテストクラス。<br/>
 *
 * @author T.Kawasaki
 */
public class MultipartHandlerTest {

    private UploadSettings settings = new UploadSettings();

    private MultipartHandler target = new MultipartHandler();

    @Before
    public void setUp() {
        settings.setAutoCleaning(true);
        target.setUploadSettings(settings);
    }

    /**
     * 正常に解析できること。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testHandle() throws IOException {

        MockServletRequest req = createMockServletRequest();
        HttpRequestHandler assertion = new HttpRequestHandler() {
            /** アップロード結果確認 */
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                //------------------ １こめ
                // name属性の値でパートが取得できること。
                List<PartInfo> uploadfile1 = request.getPart("uploadfile1");
                assertThat(uploadfile1.size(), is(2));
                PartInfo part1 = uploadfile1.get(0);
                assertThat(part1.getName(), is("uploadfile1"));
                // ファイル名が取得できること
                assertThat(part1.getFileName(), is("myFile1_ja.txt"));
                // アップロードされたファイルを読み込めること。
                List<String> lines = readAll(part1.getInputStream(), "UTF-8");
                assertThat(lines.size(), is(2));
                assertThat(lines.get(0), is("こんにちは"));
                assertThat(lines.get(1), is("さようなら"));

                //------------------ ２こめ（同じname属性のファイルが取得できること）
                PartInfo part2 = uploadfile1.get(1);
                assertThat(part2.getName(), is("uploadfile1"));
                // ファイル名が取得できること
                assertThat(part2.getFileName(), is("myFile2_ja.txt"));
                // アップロードされたファイルを読み込めること。
                lines = readAll(part2.getInputStream(), "UTF-8");
                assertThat(lines.size(), is(2));
                assertThat(lines.get(0), is("こんばんわ"));
                assertThat(lines.get(1), is("またあした"));

                //------------------ ３こめ
                // name属性の値でパートが取得できること。
                PartInfo part3 = request.getPart("uploadfile2").get(0);
                assertThat(part3.getName(), is("uploadfile2"));
                // ファイル名が取得できること
                assertThat(part3.getFileName(), is("myFile_en.txt"));
                // アップロードされたファイルを読み込めること。

                lines = readAll(part3.getInputStream(), "UTF-8");
                assertThat(lines.size(), is(2));
                assertThat(lines.get(0), is("Hello."));
                assertThat(lines.get(1), is("Good Bye."));

                //---------------- 他のパラメータがHTTPリクエストに追加されていること
                assertThat(request.getParam("username")[0], is("hoge"));
                return new HttpResponse();
            }
        };
        // 実行
        ExecutionContext context = new ExecutionContext()
                .addHandler(target)
                .addHandler(assertion);
        context.handleNext(wrap(req));
    }

    /** マルチパートでないとき、後続ハンドラに処理が委譲されること。 */
    @Test
    public void testHandleNext() {
        MockServletRequest req = new MockServletRequest();
        req.setMethod("POST");
        req.setRequestUrl("/");

        HttpRequestHandler assertion = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                context.setRequestScopedVar("hoge", "fuga");
                return new HttpResponse();
            }
        };
        ExecutionContext context = new ExecutionContext()
                .addHandler(target)
                .addHandler(assertion);
        context.handleNext(wrap(req));

        // 後続のハンドラが起動されていること。
        assertThat(context.<String>getRequestScopedVar("hoge"), is("fuga"));
    }

    /**
     * HttpRequestWrapper以外のリクエストの場合、{@link UnsupportedOperationException}が送出されること。
     */
    @Test
    public void testHandleHttpRequest() {
        ExecutionContext context = new ExecutionContext().addHandler(target);
        try {
            context.handleNext(new MockHttpRequest());
        } catch (UnsupportedOperationException e) {
            assertThat(e.getCause(), instanceOf(ClassCastException.class));
            assertThat(e.getMessage(), is("MultipartHandler expects nablarch.fw.web.servlet.HttpRequestWrapper as HttpRequest." +
                    " but was nablarch.fw.web.MockHttpRequest."));
        }
    }

    /**
     * リクエストのContent-Typeに境界文字列が見つからない場合、
     * {@link nablarch.fw.web.HttpResponse.Status#BAD_REQUEST}が返却されること。
     */
    @Test
    public void testHandleBadRequest() {
        target.setUploadSettings(settings);
        MockServletRequest req = createMockServletRequest();
        req.setContentType("multipart/form-data;");
        ExecutionContext context = new ExecutionContext().addHandler(target);
        HttpResponse res = context.handleNext(wrap(req));

        assertThat(res.getStatusCode(), is(400));
    }

    /**
     * 入出力例外が発生した場合、{@link InternalError}が送出されること。
     */
    @Test
    public void testHandleIOException() {
        MultipartHandler target = new MultipartHandler() {
            @Override
            protected MultipartParser createParser(HttpRequestWrapper wrapper, UploadSettings settings) {
                // 入出力例外を起こす解析クラスを返却
                return new MultipartParser(new MockServletInputStream(), null, settings, new MultipartContext("", -1, "UTF-8")) {
                    @Override
                    void parse(PartInfoHolder parts) throws IOException {
                        throw new IOException("for testing.");
                    }
                };
            }
        };
        target.setUploadSettings(settings);
        MockServletRequest req = createMockServletRequest();
        ExecutionContext context = new ExecutionContext().addHandler(target);

        try {
            context.handleNext(wrap(req));
            fail("入出力例外が発生した場合にINTERNAL_SERVER_ERRORが送出されること。");
        } catch (InternalError e) {
            assertThat(e.getStatusCode(), is(500));
            assertThat(e.getCause(), instanceOf(IOException.class));
        }
    }

    /**
     * アップロードサイズ上限を超過した場合、
     * {@link nablarch.fw.web.HttpResponse.Status#REQUEST_ENTITY_TOO_LARGE}が返却されること。
     */
    @Test
    public void testOverLimit() {
        UploadSettings settings = new UploadSettings();
        settings.setContentLengthLimit(300); // アップロードサイズ上限を小さくする。
        MultipartHandler target = new MultipartHandler();
        target.setUploadSettings(settings);
        MockServletRequest req = createMockServletRequest();

        ExecutionContext context = new ExecutionContext().addHandler(target);
        HttpResponse res = context.handleNext(wrap(req));

        assertThat(res.getStatusCode(), is(413));
    }

    /**
     * ファイルアップロードを処理するアクションで内部フォーワードを行った場合のテスト。
     * <p/>
     * マルチパートの解析処理が複数回行われないこと。
     * <p/>
     */
    @Test
    public void testInternalForward() {
        MockServletRequest mockServletRequest = createMockServletRequest();

        // 実行
        ExecutionContext context = new ExecutionContext()
                .setMethodBinder(new HttpMethodBinding.Binder())
                .addHandler(new ForwardingHandler())
                .addHandler(target)
                .addHandler(new RequestPathJavaPackageMapping()
                        .setBasePackage("nablarch.fw.web.upload.action")
                        .setBasePath("/test/action"));

        // 内部でForwardを行うアクションのリクエストURIを設定
        mockServletRequest.setRequestURI("//test/action/UploadAction/upload");
        // Actionの呼出
        HttpResponse response = context.handleNext(wrap(mockServletRequest));

        // 処理が正常に終わったことをAssert
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(200));
    }

    /**
     * アップロードテスト用のServletRequestを作成する。
     *
     * @return ServletRequest
     */
    private MockServletRequest createMockServletRequest() {
        MockServletRequest req = new MockServletRequest();
        req.setMethod("POST");
        req.setRequestUrl("/");
        req.setContentLength(338);
        req.setContentType("multipart/form-data; boundary=---------------------------2394118477469");
        req.setInputStream(getClass().getResourceAsStream("multipart.dat"));
        return req;
    }


    private static HttpRequestWrapper wrap(HttpServletRequest req) {
        return new HttpRequestWrapper(new NablarchHttpServletRequestWrapper(req));
    }
}