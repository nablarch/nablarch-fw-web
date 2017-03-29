package nablarch.fw.web.handler.test;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;

/**
 * Created by IntelliJ IDEA.
 *
 * @author hisaaki sioiri
 */
public class StatusTestHandler implements HttpRequestHandler {

    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        final String[] statusCode = request.getParam("statusCode");
        
        throw new Result.Error("Error") {
            @Override
            public int getStatusCode() {
                return Integer.parseInt(statusCode[0]);
            }
        };
    }
}
