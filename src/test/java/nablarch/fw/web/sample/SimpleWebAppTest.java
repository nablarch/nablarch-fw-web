package nablarch.fw.web.sample;

import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertThat;

import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.IgnoringLS;
import nablarch.test.support.tool.Hereis;

import junit.framework.TestCase;

public class SimpleWebAppTest extends TestCase {

    public void testGetIndexHtml() {
    	ThreadContext.setLanguage(null);
        HttpServer server =
            new HttpServer()
            .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
            .addHandler("/", new SimpleWebApp())
            .startLocal();
        ExecutionContext ctx = new ExecutionContext();
        HttpResponse res = server.handle(new MockHttpRequest(string()), ctx);
        /************************************
        GET /index.html HTTP/1.1
        ************************************/

        assertEquals(200, res.getStatusCode());
        assertEquals("text/html; charset=utf-8", res.getContentType());
        assertNull(res.getContentPath());
        assertThat(res.getBodyString().trim(), IgnoringLS.equals(Hereis.string().trim()));
            /****************************************
            <html>
              <head>
                <title>Greeting Service</title>
              </head>
              <body>
                Hello World!
              </body>
            </html>
            ****************************************/
    }
}
