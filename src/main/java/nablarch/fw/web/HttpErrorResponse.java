package nablarch.fw.web;

import nablarch.core.util.annotation.Published;


/**
 * エラーレスポンスを行う際に送出する例外。<br>
 * エラー時遷移先画面のパス・ステータスコードなど、HttpResponseと同等の情報を指定することができる。
 * リクエストプロセッサがこのクラスを捕捉した場合、保持しているHttpResponseオブジェクトの内容にしたがって
 * レスポンス処理が行われる。
 * 注意: 透過的トランザクションハンドラ:nablarch.common.handler.TransactionManagementHandlerを適用している場合、
 * ユーザエラーをHttpResponseオブジェクトで返却してしまうとロールバックされない。
 * HttpErrorResponseを送出することで、ユーザエラーを返しつつ、
 * トランザクションをロールバックすることが可能となる。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @see HttpResponse
 */
@Published
public class HttpErrorResponse extends RuntimeException {

    /**
     * デフォルトコンストラクタ。
     * <pre>
     * ステータスコードは400(Bad Request)を使用する。
     * このコンストラクタの処理は、以下のコードと同等である。
     *     new HttpErrorResponse(400);
     * </pre>
     */
    public HttpErrorResponse() {
        this(400);
    }

    /**
     * 元例外を指定するコンストラクタ。
     * <pre>
     * ステータスコードは400(Bad Request)を使用する。
     * このコンストラクタの処理は、以下のコードと同等である。
     *     new HttpErrorResponse(400, e);
     * </pre>
     *
     * @param e Throwable
     */
    public HttpErrorResponse(Throwable e) {
        this(400, e);
    }

    /**
     * コンテンツのパスを指定するコンストラクタ。
     * <pre>
     * ステータスコードは400(Bad Request)を使用する。
     * このコンストラクタの処理は、以下のコードと同等である。
     *     new HttpErrorResponse(400, "/error.jsp");
     * </pre>
     *
     * @param contentPath レスポンスボディに出力するコンテンツのパス
     */
    public HttpErrorResponse(String contentPath) {
        this(400, contentPath);
    }

    /**
     * コンテンツのパスと元例外を指定するコンストラクタ。
     * <pre>
     * ステータスコードは400(Bad Request)を使用する。
     * このコンストラクタの処理は、以下のコードと同等である。
     *     new HttpErrorResponse(400, "/error.jsp", e);
     * </pre>
     *
     * @param contentPath レスポンスボディに出力するコンテンツのパス
     * @param e Throwable
     */
    public HttpErrorResponse(String contentPath, Throwable e) {
        this(400, contentPath, e);
    }

    /**
     * 指定されたステータスコードでエラーレスポンスを返す例外を生成する。
     *
     * @param statusCode ステータスコード
     */
    public HttpErrorResponse(int statusCode) {
        super();
        this.response = new HttpResponse(statusCode);
    }

    /**
     * 指定されたステータスコードでエラーレスポンスを返す例外を生成する。
     *
     * @param statusCode ステータスコード
     * @param e 元例外
     */
    public HttpErrorResponse(int statusCode, Throwable e) {
        super(e);
        this.response = new HttpResponse(statusCode);
    }

    /**
     * 指定されたステータスコード・コンテンツパスでエラーレスポンスを返す例外を生成する。
     *
     * @param statusCode ステータスコード
     * @param contentPath レスポンスボディに出力するコンテンツのパス
     */
    public HttpErrorResponse(int statusCode, String contentPath) {
        super();
        this.response = new HttpResponse(statusCode, contentPath);
    }

    /**
     * 指定されたステータスコード・コンテンツパスでエラーレスポンスを返す例外を生成する。
     *
     * @param statusCode ステータスコード
     * @param contentPath レスポンスボディに出力するコンテンツのパス
     * @param e 元例外
     */
    public HttpErrorResponse(int statusCode, String contentPath, Throwable e) {
        super(e);
        this.response = new HttpResponse(statusCode, contentPath);
    }

    /**
     * 指定された{@link HttpResponse}を持つ{@code HttpErrorResponse}を生成する。
     *
     * @param response {@link HttpResponse}
     */
    public HttpErrorResponse(HttpResponse response) {
        this.response = response;
    }

    /**
     * 指定された{@link HttpResponse}と例外を持つ{@code HttpErrorResponse}を生成する。
     *
     * @param response {@link HttpResponse}
     * @param e 元例外
     */
    public HttpErrorResponse(HttpResponse response, Throwable e) {
        super(e);
        this.response = response;
    }

    /**
     * レスポンス情報を取得する。
     *
     * @return レスポンス情報。
     */
    public HttpResponse getResponse() {
        return this.response;
    }

    /**
     * レスポンス情報を設定する。
     *
     * @param response レスポンス情報
     * @return このオブジェクト自身
     */
    public HttpErrorResponse setResponse(HttpResponse response) {
        this.response = response;
        return this;
    }

    /** レスポンス情報 */
    private HttpResponse response;
}
