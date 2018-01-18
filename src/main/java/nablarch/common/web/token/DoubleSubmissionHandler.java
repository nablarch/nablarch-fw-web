package nablarch.common.web.token;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * OnDoubleSubmissionアノテーションに対する処理を行うインタフェース。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public interface DoubleSubmissionHandler {
    /**
     * OnDoubleSubmissionアノテーションに対する処理を行う。
     * @param request HTTPリクエストオブジェクト
     * @param context サーバサイド実行コンテキストオブジェクト
     * @param httpRequestHandler 処理対象のリクエストハンドラ
     * @param annotation 処理対象のOnDoubleSubmission
     * @return HTTPレスポンスオブジェクト
     */
    HttpResponse handle(
        HttpRequest        request,
        ExecutionContext   context,
        Handler<HttpRequest, HttpResponse> httpRequestHandler,
        OnDoubleSubmission annotation
    );
}
