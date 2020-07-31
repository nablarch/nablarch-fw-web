package nablarch.common.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.PassThroughHandler;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * {@link PassThroughHandler}のテスト
 */
public class PassThroughHandlerTest {
    @Test
    public void test() {
        PassThroughHandler handler = new PassThroughHandler();
        ExecutionContext context = new ExecutionContext()
                .clearHandlers()
                .addHandler(handler)
                .addHandler(new FinalHandler());
        HttpResponse response = context.handleNext(new MockRequest());
        assertThat(response.getStatusCode(), is(200));
    }

    private static class FinalHandler implements HttpRequestHandler {
        @Override
        public HttpResponse handle(HttpRequest httpRequest, ExecutionContext executionContext) {
            return new HttpResponse();
        }
    }

    private static class MockRequest extends HttpRequest {
        @Override
        public String getMethod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHttpVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String[]> getParamMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getParam(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpRequest setParam(String s, String... strings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpRequest setParamMap(Map<String, String[]> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getHeaderMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getHeader(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpCookie getCookie() {
            throw new UnsupportedOperationException();
        }
    }
}