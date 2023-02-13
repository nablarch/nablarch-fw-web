package nablarch.fw.web.handler;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpResponseHandler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * {@link HttpResponseHandler#isErrorResponse(HttpResponse)}}のテストクラス。
 */
@RunWith(Parameterized.class)
public class HttpResponseHandlerIsErrorResponseTest {

    @Parameters
    public static List<Param> parameters() {
        return Arrays.asList(
                // 以下正常
                new Param(100, false),
                new Param(200, false),
                new Param(204, false),
                new Param(301, false),
                new Param(302, false),
                new Param(309, false),

                // 以下異常
                new Param(400, true),
                new Param(404, true),
                new Param(500, true),
                new Param(503, true),
                new Param(599, true),
                new Param(600, true)
        );
    }

    private final Param param;

    private final HttpResponseHandler sut = new HttpResponseHandler();

    public HttpResponseHandlerIsErrorResponseTest(final Param param) {
        this.param = param;
    }

    @Test
    public void testIsErrorResponse() throws Exception {
        assertThat(param.toString(), sut.isErrorResponse(param.httpResponse), is(param.expected));
    }

    private static class Param {

        private final HttpResponse httpResponse;

        private final boolean expected;

        public Param(final int statusCode, final boolean expected) {
            this.httpResponse = new HttpResponse() {
                @Override
                public int getStatusCode() {
                    return statusCode;
                }
            };
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "Param{" +
                    "status code=" + httpResponse.getStatusCode() +
                    ", expected=" + expected +
                    '}';
        }
    }
}
