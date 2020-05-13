package nablarch.fw.web.handler.csrf;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * CSRFトークンの検証失敗時の処理を行うインタフェース。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public interface VerificationFailureHandler {

    /**
     * CSRFトークンの検証失敗時の処理を行う。
     *
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @param userSentToken ユーザーが送信したトークン
     * @param sessionAssociatedToken セッションに格納されたトークン
     * @return HTTPレスポンス
     */
    HttpResponse handle(
            HttpRequest request, ExecutionContext context, String userSentToken, String sessionAssociatedToken);
}
