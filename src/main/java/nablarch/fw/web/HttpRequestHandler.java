package nablarch.fw.web;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;

/**
 * HTTPリクエストに対して何らかの処理を行うモジュールが実装するインターフェース。
 * <pre>
 * このインターフェースを実装したクラスのインスタンスは、リクエストスレッド間で共有される可能性がある。
 * その場合はスレッド競合を意識した実装を要する。
 * </pre>
 *
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public interface HttpRequestHandler extends Handler<HttpRequest, HttpResponse> {
    /**
     * HTTPリクエストに対する処理を実行する。
     *
     * @param request HTTPリクエストオブジェクト
     * @param context サーバサイド実行コンテキストオブジェクト
     * @return HTTPレスポンスオブジェクト
     */
    HttpResponse handle(HttpRequest request, ExecutionContext context);
}
