package nablarch.fw.web;

import static org.junit.Assert.assertEquals;

import nablarch.fw.ExecutionContext;
import org.junit.Test;

public class HttpRequestProcessorTest {
	@Test
    public void testAddObjectAsHandler() {
    	ExecutionContext context = new ExecutionContext()
    	.setMethodBinder(new HttpMethodBinding.Binder())
    	.addHandler(new Object() {
    	    public HttpResponse postMessage(HttpRequest req, ExecutionContext ctx) {
    	        return new HttpResponse(201);
    	    }
    	});
    	HttpResponse res = context.handleNext(new MockHttpRequest("POST /message HTTP/1.1"));
        assertEquals(201, res.getStatusCode());
    }
}
