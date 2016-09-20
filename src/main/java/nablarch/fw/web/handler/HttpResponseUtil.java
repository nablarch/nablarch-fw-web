package nablarch.fw.web.handler;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTP の応答時に使用するユーティリティ。 
 *
 * @author Koichi Asano 
 *
 */
public final class HttpResponseUtil {

    /**
     * HTTPレスポンスコードの変換方法。
     * @author Masaya Seko
     */
    public enum StatusConvertMode {
        /** Nablarchのステータスコードを、HTTPレスポンスコード200に変換する(例外的に300系は除く)。 */
        CONVERT_ALL_TO_200,
        /** Nablarchのステータスコード400のみ、HTTPレスポンスコード200に変換する。 */
        CONVERT_ONLY_400_TO_200
    }

    /** HTTPレスポンスコードの変換方法をコンテキストから取得する際のキー。*/
    private static final String HTTP_STATUS_CONVERT_MODE_KEY = "nablarch_http_status_convert_mode";

    /**
     * 隠蔽コンストラクタ
     */
    private HttpResponseUtil() {
        
    }

    /**
     * Nablarchのステータスコードから、
     * クライアントに返すレスポンスに使用するHTTPステータスコードを選択する。
     * 
     * ステータスコードの値は以下のルールに従って決定される。
     * 
     * <p/>
     * 1. 通常の画面遷移の場合
     * <p/>
     * HTTPレスポンスコードの変換モード(通常は{@link HttpResponseHandler}経由で設定する)が、CONVERT_ONLY_400_TO_200の場合、
     * 以下のようにステータスコードのマッピングを行う。<br>
     * このマッピングは、1.5.x以降のデフォルトのマッピングである。<br>
     * バリデーションエラー等を表すNablarchのステータスコード400番のみ、HTTPレスポンスコード200にマッピングされる。<br>
     * <pre>
     * ========================= =======================
     * Nablarchのステータスコード    HTTPレスポンスコード
     * ========================= =======================
     * 2xx                       (そのまま)
     * 3xx                       (そのまま)
     * 400                       200
     * 4xx(400以外)              (そのまま)
     * 5xx                       (そのまま)
     * ========================= =======================
     * </pre>
     *
     * HTTPレスポンスコードの変換モードが、CONVERT_ALL_TO_200の場合、
     * 以下のようにステータスコードのマッピングを行う。<br>
     * このマッピングは、1.4.x以前と同一のマッピングである。<br>
     *
     * <pre>
     * ========================= =======================
     * Nablarchのステータスコード    HTTPレスポンスコード
     * ========================= =======================
     * 2xx                       200
     * 3xx                       (そのまま)
     * 4xx                       200
     * 5xx                       200
     * ========================= =======================
     * 
     * </pre>
     * 2. Ajaxクライアントからのリクエストの場合
     * <p/>
     * HTTPリクエストに ヘッダ X-Requested-With が設定されており、
     * かつその値が、"XMLHttpRequest" に一致する場合はHTTPレスポンスオブジェクトの
     * ステータスコードをそのまま設定する。
     * <p/>
     * X-Requested-Withヘッダとは、javascriptフレームワークが XMLHttpRequestオブジェクトを
     * 使用してリクエストを送信する際に付与するカスタムHTTPヘッダであり、
     * 主要なJavascriptフレームワークのほとんどでこれをサポートしている。
     * (prototype.js, jQuery, dojo, MooTools など)
     * <p/>
     * また、XMLHttpRequestを直接使用している場合でも、以下のサンプルの様なコードを
     * 追加することで、簡単に対応させることができる。 
     * 
     * <pre>
     * var xhr = getXmlHttpRequest(); // XMLHttpRequestオブジェクトを生成
     * xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest"); // ヘッダ追加
     * </pre>
     * 
     * @param res HttpResponse
     * @param ctx ServletExecutionContext
     * @return クライアントに返すレスポンスに使用するHTTPステータスコード
     */
    public static int chooseResponseStatusCode(HttpResponse res,
            ServletExecutionContext ctx) {
        int statusCode;
        if (isAjaxRequest(ctx)) {
            // Ajaxリクエストの場合はステータスコードをそのままレスポンスに設定。
            statusCode = res.getStatusCode();
        } else {
            if (ctx.getRequestScopedVar(HTTP_STATUS_CONVERT_MODE_KEY) == StatusConvertMode.CONVERT_ALL_TO_200) {
                //1.4以前の仕様

                if (res != null && 300 <= res.getStatusCode() && res.getStatusCode() <= 399) {
                    // 3xx 系のステータスコードはそのまま返却
                    statusCode = res.getStatusCode();
                } else {
                    // それ以外はステータスコード200に変換
                    statusCode = 200;
                }
            } else {
                //1.5以降の仕様

                if (res == null || 400 == res.getStatusCode()) {
                    // 以下の場合は200にマッピング
                    // ・レスポンスのオブジェクトがnull
                    // ・400 (バリデーションエラー、改ざん検知等を表す)
                    statusCode = 200;
                } else {
                    statusCode = res.getStatusCode();
                }
            }
        }
        return statusCode;
    }

    /**
     * このリクエストがXMLHttpRequestオブジェクト(いわゆるAjaxリクエスト)によるものか
     * どうかを判定する。
     * 
     * このメソッドでは、HTTPリクエスト中に含まれる X-Requested-With ヘッダを用いて
     * Ajaxリクエストの判定を行う。
     * 
     * @param ctx 実行コンテキスト
     * @return Ajaxリクエストであれば true を返す。
     */
    public static boolean isAjaxRequest(ServletExecutionContext ctx) {
        return "XMLHttpRequest".equals(ctx.getServletRequest().getHeader(
                "X-Requested-With"));
    }

    /**
     * NablarchのステータスコードをHTTPレスポンスコードに変換する際のモードを設定する。
     * @param ctx 実行コンテキスト
     * @param mode 変換のモード
     */
    public static void setStatusConvertMode(ServletExecutionContext ctx, StatusConvertMode mode) {
        ctx.setRequestScopedVar(HTTP_STATUS_CONVERT_MODE_KEY, mode);
    }

}
