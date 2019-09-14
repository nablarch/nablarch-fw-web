package nablarch.fw.web.sample;

import nablarch.TestUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;

/**
 * Nablaarchで構築された単純なアプリケーションの例
 * 
 * @author Iwauo Tajima
 */
public class SimpleWebApp {
    /**
     * このメソッドは、以下のHTTPリクエストに対して呼び出される。
     * <pre>
     *   GET /(ベースURI)/index.html
     * </pre>
     * リクエストスコープに画面に表示する文字列を設定した上で、
     * "/jsp/index.jsp" にフォーワードし、その結果を返す。
     *
     * @param req HTTPリクエストオブジェクト
     * @param ctx サーバサイド情報オブジェクト
     * @return HTTPレスポンスオブジェクト
     */
    public HttpResponse getIndexHtml(HttpRequest req, ExecutionContext ctx) {
        ctx.setRequestScopedVar("greeting", "Hello World!");
        return new HttpResponse(200, "servlet://jsp/index.jsp");
    }
    
    // マルチバイト文字を含んだリソース名
    public HttpResponse get画像Png(HttpRequest req, ExecutionContext stx) {
        return new HttpResponse(200, "servlet://img/secret.png");
    }
    
    /**
     * 本サンプルアプリケーションをデプロイしたサーバを
     * ポート8090上で起動する。
     */
    public static final void main (String[] argsv) {
        TestUtil.createHttpServer()
            .setPort(8090)
            .setWarBasePath("classpath://nablarch/fw/web/sample/")
            .addHandler("/app/", new SimpleWebApp())
            .start()
            .join();
    }
}