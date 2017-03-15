package nablarch.fw.web.handler;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.MockServletResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;
import org.junit.After;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpCharacterEncodingHandler} のテストクラス。
 *
 * @author Toru Nagashima
 */
public class HttpCharacterEncodingHandlerTest {

    /**
     * {@link HttpCharacterEncodingHandler#setDefaultEncodingCharset(Charset)} のテスト。<br />
     * エンコーディングを明示的に指定できることを確認する。
     */
    @Test
    public void testAccessorsToCharacterEncoding() {
        HttpCharacterEncodingHandler target = new HttpCharacterEncodingHandler();
        assertThat(target.getDefaultEncoding(), is(Charset.forName("UTF-8")));

        target.setDefaultEncodingCharset(Charset.forName("SJIS"));
        assertThat(target.getDefaultEncoding(), is(Charset.forName("Shift-JIS")));

        target.setDefaultEncoding("MS932");
        assertThat(target.getDefaultEncoding(), is(Charset.forName("windows-31j")));
    }

    /**
     * {@link HttpCharacterEncodingHandler#setDefaultEncoding(String)} のテスト。<br />
     * コンポーネント定義ファイルにて、エンコーディングを明示的に指定できることを確認する。
     */
    @Test
    public void testSetEncoding() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
            "nablarch/fw/web/handler/override-default-encoding.xml"
        );
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        HttpCharacterEncodingHandler target = SystemRepository.get("httpCharacterEncodingHandler");

        assertThat(target.getDefaultEncoding(), is(Charset.forName("Windows-31J")));
    }

    /**
     * {@link HttpCharacterEncodingHandler#handle(Object, ExecutionContext)} のテスト。<br />
     * デフォルトエンコーディングのまま実行されるケース。<br />
     * CharacterEncodingを出力する設定にして検証する。<br />
     * デフォルトのエンコーディング（UTF-8）が ServletRequest/ServletResponse に設定されていることを検証する。<br />
     *
     * @throws Exception 想定していない例外が発生した場合。
     */
    @Test
    public void testHandleDefaultEncoding() throws Exception {
        HttpCharacterEncodingHandler target = new HttpCharacterEncodingHandler();
        target.setAppendResponseCharacterEncoding(true);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = createMockServletResponse();
        request.setCharacterEncoding(null);
        response.setCharacterEncoding(null);
        MockServletContext servletContext = new MockServletContext();

        ServletExecutionContext context = new ServletExecutionContext(request, response, servletContext);
        context.addHandler(target).addHandler(new FinalHandler());
        context.handleNext(new MockHttpRequest());

        assertThat("ServletRequest encoding", request.getCharacterEncoding(),
                is(Charset.forName("UTF-8").name()));
        assertThat("ServletResponse encoding", response.getCharacterEncoding(),
                is(Charset.forName("UTF-8").name()));
    }

    /**
     * {@link HttpCharacterEncodingHandler#handle(Object, ExecutionContext)} のテスト。<br />
     * デフォルトのエンコーディングを上書きするケース。<br />
     * CharacterEncodingを出力する設定にして検証する。<br />
     * 上書きしたエンコーディングが ServletRequest/ServletResponse に設定されていることを検証する。<br />
     *
     * @throws Exception 想定していない例外が発生した場合。
     */
    @Test
    public void testHandleOverrideEncoding() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/web/handler/override-default-encoding.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        HttpCharacterEncodingHandler target = SystemRepository
                .get("httpCharacterEncodingHandler");
        target.setAppendResponseCharacterEncoding(true);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = createMockServletResponse();
        request.setCharacterEncoding(null);
        response.setCharacterEncoding(null);
        MockServletContext servletContext = new MockServletContext();

        ServletExecutionContext context = new ServletExecutionContext(request, response, servletContext);
        context.addHandler(target).addHandler(new FinalHandler());
        context.handleNext(new MockHttpRequest());

        assertThat("ServletRequest encoding", request.getCharacterEncoding(),
                is(Charset.forName("Windows-31J").name()));
        assertThat("ServletResponse encoding", response.getCharacterEncoding(),
                is(Charset.forName("Windows-31J").name()));
    }

    /**
     * {@link HttpCharacterEncodingHandler#handle(Object, ExecutionContext)} のテスト。<br />
     * CharacterEncodingを出力しない設定にして検証する。<br />
     * デフォルトの設定値（出力しない）を使用する。<br />
     * ServletResponse に設定されないことを検証する。<br />
     *
     * @throws Exception 想定していない例外が発生した場合。
     */
    @Test
    public void testHandleNotAppendCharacterEncoding() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/fw/web/handler/override-default-encoding.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
        HttpCharacterEncodingHandler target = SystemRepository
                .get("httpCharacterEncodingHandler");

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = createMockServletResponse();
        request.setCharacterEncoding(null);
        response.setCharacterEncoding(null);
        MockServletContext servletContext = new MockServletContext();

        ServletExecutionContext context = new ServletExecutionContext(request, response, servletContext);
        context.addHandler(target).addHandler(new FinalHandler());
        context.handleNext(new MockHttpRequest());

        assertThat("ServletRequest encoding", request.getCharacterEncoding(),
                is(Charset.forName("Windows-31J").name()));
        assertNull("ServletResponse encoding", response.getCharacterEncoding());
    }

    @After
    public void clearRepository() {
        SystemRepository.clear();
    }

    /**
     * テスト用の ServletResponse を生成する。<br />
     * characterEncoding の設定有無が確認できるように、{@link MockServletResponse} を拡張する。<br />
     *
     * @return テスト用の ServletResponse　
     */
    private MockServletResponse createMockServletResponse() {
        return new MockServletResponse() {

            private String characterEncoding;

            @Override
            public void setCharacterEncoding(String arg0) {
                characterEncoding = arg0;
            }

            @Override
            public String getCharacterEncoding() {
                return characterEncoding;
            }
        };
    }

    /**
     * テスト用の終端ハンドラ。<br />
     * {@link HttpCharacterEncodingHandler#handle(Object, ExecutionContext)} のテストをする時に使用する。<br />
     *
     * @author Toru Nagashima
     */
    private static class FinalHandler implements HttpRequestHandler {
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            return new HttpResponse(200);
        }
    }

}
