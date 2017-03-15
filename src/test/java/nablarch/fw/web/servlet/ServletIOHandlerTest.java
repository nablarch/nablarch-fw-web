package nablarch.fw.web.servlet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderFactory;
import nablarch.fw.web.handler.HttpResponseHandler;
import org.junit.Test;

public class ServletIOHandlerTest {

    @Test
    public void testSettingFileNameEncoding() {
        final DownloadFileNameEncoder zapEncoder = new DownloadFileNameEncoder() {
            public String encode(String fileName) {
                return "zap! zap!";
            }
        };
        DownloadFileNameEncoderFactory zapEncoderFactory = new DownloadFileNameEncoderFactory() {
            @Override
            public DownloadFileNameEncoder getEncoder(String ua) {
                return zapEncoder;
            }
        };
        
        HttpServer handler = new HttpServer().setHandlerQueue((Collection) Arrays.asList(new Handler[] {
            new HttpResponseHandler().setDownloadFileNameEncoderFactory(zapEncoderFactory)
          , new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest req, ExecutionContext ignored) {

                    return new HttpResponse()
                            .setContentDisposition("hogehoge")
                            // bodyが空だとエラーになるので
                            .setBodyStream(new ByteArrayInputStream("hogehoge".getBytes()));
                }
            }
        })).startLocal();
        
        HttpResponse res = handler.handle(new MockHttpRequest(), null);
        String contentDisposition = res.getContentDisposition();
        assertThat(contentDisposition, is(not(nullValue())));
        assertTrue(contentDisposition.contains("filename=\"zap! zap!\""));
    }
}
