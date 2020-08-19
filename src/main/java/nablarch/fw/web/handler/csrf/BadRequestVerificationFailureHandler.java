package nablarch.fw.web.handler.csrf;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * CSRFトークンの検証失敗時にBadRequest(400)のレスポンスを返すクラス。
 *
 * INFOレベルで検証失敗時のログを出力する。
 *
 * @author Kiyohito Itoh
 */
public class BadRequestVerificationFailureHandler implements VerificationFailureHandler {

    /** ロガー */
    private static Logger LOGGER = LoggerManager.get(BadRequestVerificationFailureHandler.class);

    @Override
    public HttpResponse handle(
            HttpRequest request, ExecutionContext context, String userSentToken, String sessionAssociatedToken) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.logInfo(
                    String.format("CSRF token verification failed. userSentToken=[%s] sessionAssociatedToken=[%s]",
                            userSentToken, sessionAssociatedToken));
        }
        return HttpResponse.Status.BAD_REQUEST.handle(request, context);
    }
}
