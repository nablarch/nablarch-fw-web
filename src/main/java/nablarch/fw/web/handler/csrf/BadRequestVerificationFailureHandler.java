package nablarch.fw.web.handler.csrf;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * CSRFトークンの検証失敗時にBadRequest(400)のレスポンスを返すクラス。
 *
 * @author Kiyohito Itoh
 */
public class BadRequestVerificationFailureHandler implements VerificationFailureHandler {
    @Override
    public HttpResponse handle(
            HttpRequest request, ExecutionContext context, String userSentToken, String sessionAssociatedToken) {
        return HttpResponse.Status.BAD_REQUEST.handle(request, context);
    }
}
