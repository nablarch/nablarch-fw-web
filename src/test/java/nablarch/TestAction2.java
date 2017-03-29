package nablarch;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

public class TestAction2 {
    public HttpResponse getMessage(HttpRequest data, ExecutionContext context) {
        return new HttpResponse(200).write("this is a test message.");
    }
}
