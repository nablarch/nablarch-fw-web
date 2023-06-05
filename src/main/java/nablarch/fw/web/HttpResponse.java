package nablarch.fw.web;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.Cookie;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;

/**
 * HTTPレスポンスメッセージを生成する際に必要な情報を格納したクラス。
 * <p/>
 * HTTPクライアントに対するレスポンス処理をフレームワークが行うために必要な
 * 以下の情報を保持する。
 * <ul>
 *     <li>レスポンスステータスコード</li>
 *     <li>レスポンスヘッダ</li>
 *     <li>レスポンスボディ</li>
 * </ul>
 * レスポンスボディの内容は、以下のいずれかの方式によって保持する。
 * <ol>
 *     <li>内容をこのオブジェクトに直接保持する方式(バッファ方式)</li>
 *     <li>ボディに書き込むコンテンツファイルのパスのみを指定する方式(コンテンツパス方式)</li>
 * </ol>
 * {@link #setContentPath(String)} の値を設定することで後者の方式がとられるようになる。<br/>
 * メモリ消費の観点や、コンテンツファイル管理の容易さから、
 * 通常はコンテンツパスによる方式を使用すべきである。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @author Masato Inoue
 */
public class HttpResponse implements Result {
    /** HTTP行終端文字(CRLF) */
    public static final String LS = "\r\n";
    /** アスキーエンコーディング */
    private static final Charset ASCII = Charset.forName("iso-8859-1");
    /** UTF-8エンコーディング */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /** Content-Typeヘッダのcharsetが設定されたパターン */
    private static final Pattern CHARSET_ATTR_IN_CONTENT_PATH = Pattern.compile(
        "^.*?;\\s*charset=\\s*\"?(.*?)\"?\\s*$(;.*)?"
    );

    /**
     * HTTPレスポンスステータス。
     * <pre>
     * HTTPレスポンスフォーマットにおけるHTTPステータスコード・ステータスフレーズの値と
     * それに関連する責務を実装した{@link Enum}。
     * </pre>
     *
     * @author Iwauo Tajima <iwauo@tis.co.jp>
     */
    @Published
    public static enum Status implements HttpRequestHandler {
        /** 継続 */
        CONTINUE(100),

        /** 正常終了 */
        OK(200),

        /** 正常終了:リソースが正常に作成された。 */
        CREATED(201),

        /** 正常終了:処理の受付が完了した。(後続処理がある場合) */
        ACCEPTED(202),

        /** 内容なし */
        NO_CONTENT(204),

        /** 内容のリセット */
        RESET_CONTENT(205),

        /** 部分的内容 */
        PARTIAL_CONTENT(206),

        /** 恒久的に移動した。 */
        MOVED_PERMANENTLY(301),

        /** リダイレクト */
        FOUND(302),

        /** リダイレクト */
        SEE_OTHER(303),

        /** コンテンツに変更が無い。 */
        NOT_MODIFIED(304),

        /** プロキシを使用せよ。 */
        USE_PROXY(305),

        /** 一時的リダイレクト */
        TEMPORARY_REDIRECT(307),

        /** ユーザエラー:リクエストの内容に問題があるので処理を継続できない。 */
        BAD_REQUEST(400),

        /** 未認証 */
        UNAUTHORIZED(401),

        /** 支払いが必要である。 */
        PAYMENT_REQUIRED(402),

        /** 認証・認可エラー */
        FORBIDDEN(403),

        /** ユーザエラー:リクエストURIに相当するリソースが存在しない。 */
        NOT_FOUND(404),

        /** 許可されていないメソッド */
        METHOD_NOT_ALLOWED(405),

        /** 受理できない。 */
        NOT_ACCEPTABLE(406),

        /** プロキシ認証が必要である。 */
        PROXY_AUTHENTICATION_REQUIRED(407),

        /** リクエストタイムアウト */
        REQUEST_TIMEOUT(408),

        /** リソース競合エラー */
        CONFLICT(409),

        /** 消滅した。 */
        GONE(410),

        /** 長さが必要。 */
        LENGTH_REQUIRED(411),

        /** 前提条件で失敗した。 */
        PRECONDITION_FAILED(412),

        /** リクエストエンティティが大きすぎる。*/
        REQUEST_ENTITY_TOO_LARGE(413),

        /** URIが大きすぎる。 */
        REQUEST_URI_TOO_LONG(414),

        /** サポートしていないメディアタイプ */
        UNSUPPORTED_MEDIA_TYPE(415),

        /** レンジは範囲外にある。 */
        REQUESTED_RANGE_NOT_SATISFIABLE(416),

        /** Expectヘッダによる拡張が失敗。 */
        EXPECTATION_FAILED(417),

        /** システムエラー:システム上の問題が発生したため処理を継続できない。 */
        INTERNAL_SERVER_ERROR(500),

        /** 実装されていない。 */
        NOT_IMPLEMENTED(501),

        /** 不正なゲートウェイ */
        BAD_GATEWAY(502),

        /** システムエラー:サービスを一時的に停止している。 */
        SERVICE_UNAVAILABLE(503),

        /** ゲートウェイタイムアウト */
        GATEWAY_TIMEOUT(504),

        /** サポートしていないHTTPバージョン */
        HTTP_VERSION_NOT_SUPPORTED(505);

        /** ステータスコードをキーとするインデックス */
        private static final Map<Integer, Status>
                INDEX_BY_CODE = new HashMap<Integer, Status>();

        static {
            for (Status status : Status.values()) {
                INDEX_BY_CODE.put(status.code, status);
            }
        }

        /** HTTPステータスコード */
        private final int code;

        /** HTTPレスポンスフレーズ */
        private final String phrase;

        /**
         * 指定したステータスコードのオブジェクトを生成する。
         *
         * @param code HTTPステータスコード
         */
        private Status(int code) {
            this.code = code;
            this.phrase = this.name();
        }

        /**
         * 指定されたステータスコードに対する{@code Status}オブジェクトを返す。
         *
         * @param code HTTPステータスコード
         * @return Statusオブジェクト
         * @throws IllegalArgumentException ステータスコードが{@code Status}オブジェクトに含まれていない場合
         */
        public static Status valueOfCode(int code) throws IllegalArgumentException {
            Status status = INDEX_BY_CODE.get(code);
            if (status == null) {
                throw new IllegalArgumentException("invalid status code.[" + code + "]");
            }
            return status;
        }

        /**
         * 入力データに対する処理を実行する。
         * <p/>
         * このクラスの実装では、以下のHTTPレスポンスメッセージに相当する
         * {@code HttpResponse}オブジェクトを返す。
         * <pre>
         *     HTTP/1.1 (ステータスコード) (ステータスフレーズ)
         *     Content-Type: text/plain;charset=UTF-8
         * </pre>
         *
         * @param req 入力データ
         * @param ctx 実行コンテキスト
         * @return 本オブジェクト
         */
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            return new HttpResponse()
                  .setStatusCode(this.code);
        }

        /**
         * HTTPステータスコードを取得する。
         *
         * @return HTTPステータスコード
         */
        public int getStatusCode() {
            return code;
        }
    }

    /**
     * {@code HttpResponse}オブジェクトを生成する。
     * <p/>
     * 以下のHTTPレスポンスメッセージに相当する{@code HttpResponse}オブジェクトを生成する。
     * <pre>
     *     HTTP/1.1 200 OK
     *     Content-Type: text/plain;charset=UTF-8
     * </pre>
     */
    @Published
    public HttpResponse() {
        this(200);
    }

    /**
     * 指定されたステータスコードの{@code HttpResponse}オブジェクトを生成する。
     * <p/>
     * このメソッドの処理は以下のソースコードと等価である。
     * <code><pre>
     *     new HttpResponse().setStatusCode(statusCode);
     * </pre></code>
     *
     * @param statusCode HTTPステータスコード
     */
    @Published
    public HttpResponse(int statusCode) {
        this.setStatusCode(statusCode);
        this.headers = new HashMap<String, String>();
        this.cookies = new ArrayList<Cookie>();
    }

    /**
     * 指定されたHTTPステータスコードとコンテンツパスの{@code HttpResponse}オブジェクトを生成する。
     * <p/>
     * このメソッドの処理は以下のソースコードと等価である。
     * <code><pre>
     *     new HttpResponse().setStatusCode(statusCode)
     *                       .setContentPath(contentPath);
     * </pre></code>
     *
     * @param statusCode HTTPステータスコード
     * @param contentPath コンテンツパス
     */
    @Published
    public HttpResponse(int statusCode, String contentPath) {
        this(statusCode);
        this.setContentPath(contentPath);
    }

    /**
     * 指定されたコンテンツパスの{@code HttpResponse}オブジェクトを生成する。
     * <p/>
     * このメソッドの処理は以下のソースコードと等価である。
     * <code><pre>
     *     new HttpResponse().setStatusCode(200)
     *                       .setContentPath(contentPath);
     * </pre></code>
     *
     * @param contentPath コンテンツパス
     */
    @Published
    public HttpResponse(String contentPath) {
        this(200, contentPath);
    }

    /**
     * HTTPレスポンスメッセージの内容から{@code HttpResponse}オブジェクトを生成する。
     * @param message HTTPレスポンスメッセージ
     * @return HTTPレスポンスメッセージの内容に対応した{@code HttpResponse}オブジェクト
     */
    public static HttpResponse parse(String message) {
        HttpResponse result = new HttpResponse();
        result.parseMessage(new StringReader(message));
        return result;
    }

    /**
     * HTTPレスポンスメッセージの内容から{@code HttpResponse}オブジェクトを生成する。
     * @param message HTTPレスポンスメッセージ
     * @return HTTPレスポンスメッセージの内容に対応した{@code HttpResponse}オブジェクト
     */
    public static HttpResponse parse(byte[] message) {
        HttpResponse result = new HttpResponse();
        result.wasBytes = true;
        result.parseMessage(new StringReader(StringUtil.toString(message, ASCII)));
        return result;
    }
    /** HTTPメッセージをbyte配列から作成したかどうか */
    private boolean wasBytes = false;

    /**
     * HTTPレスポンスのステータスコードの値を返す。
     * <p/>
     * HTTPレスポンスがリダイレクトである場合は{@code 302}を返す。
     *
     * @return HTTPステータスコード
     * @see #setStatusCode(int)
     */
    @Published
    public int getStatusCode() {
        if (body != null && body.getContentPath() != null && body.getContentPath().isRedirect() && !isRedirectStatusCode()) {
            return Status.FOUND.code;
        }
        return this.status.code;
    }

    /**
     * {@link #status}がリダイレクトを示すステータスコードかどうか。
     * @return リダイレクトを示すステータスコード(301, 302, 303, 307)の場合はtrue
     */
    private boolean isRedirectStatusCode() {
        return status == Status.MOVED_PERMANENTLY
                || status == Status.FOUND
                || status == Status.SEE_OTHER
                || status == Status.TEMPORARY_REDIRECT;
    }

    /**
     * HTTPレスポンスのステータスコードを設定する。
     * <p/>
     * デフォルトのステータスコードは{@code 200}である。
     *
     * @param code HTTPステータスコード
     * @return 本オブジェクト
     * @throws IllegalArgumentException 指定されたステータスコードが無効な場合
     */
    @Published
    public HttpResponse setStatusCode(int code) {
        if (code < 100 || code > 999) {
            throw new IllegalArgumentException("invalid status code:" + code);
        }
        this.status = Status.valueOfCode(code);
        return this;
    }

    /** HTTPレスポンスステータス */
    private Status status = Status.OK;

    /**
     * HTTPレスポンスのステータスフレーズを返す。
     *
     * @return ステータスフレーズ
     */
    @Published
    public String getReasonPhrase() {
        return this.status.phrase;
    }

    /**
     * 処理結果に対する詳細情報を返す。
     * <p/>
     * 返される詳細情報は以下の通りである。
     * <pre>
     *   (ステータスコード): (ステータスフレーズ)
     * </pre>
     */
    @Published
    public String getMessage() {
        return String.valueOf(this.status.code) + ": " + getReasonPhrase();
    }

    /**
     * HTTPバージョンを表す文字列を返す。
     *
     * @return HTTPバージョン名
     */
    @Published
    public String getHttpVersion() {
        return this.httpVersion;
    }

    /**
     * HTTPバージョンを設定する。
     * <p/>
     * デフォルト値は "HTTP/1.1" である。
     *
     * @param httpVersion HTTPバージョン名
     * @return 本オブジェクト
     * @throws IllegalArgumentException HTTPバージョンの書式が無効な場合
     */
    public HttpResponse setHttpVersion(String httpVersion) {
        if (!HTTP_VERSION_SYNTAX.matcher(httpVersion).matches()) {
            throw new IllegalArgumentException("invalid : " + httpVersion);
        }
        this.httpVersion = httpVersion;
        return this;
    }

    /** HTTPプロトコルバージョン */
    private String httpVersion = "HTTP/1.1";

    /** HTTPバージョンの書式 */
    private static final Pattern HTTP_VERSION_SYNTAX = Pattern.compile(
        "HTTP/(0\\.9|1\\.0|1\\.1)"
    );

    /**
     * HTTPレスポンスヘッダを格納するMapを返す。
     * <p/>
     * このMapに対する変更はレスポンスヘッダの内容に直接反映される。
     *
     * @return HTTPレスポンスヘッダを格納するMap
     */
    @Published
    public Map<String, String> getHeaderMap() {
        getContentLength();
        getContentType();
        return this.headers;
    }

    /**
     * HTTPレスポンスヘッダの値を返す。
     * @param headerName ヘッダー名
     * @return ヘッダーの値
     */
    @Published
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    /**
     * HTTPレスポンスヘッダの値を設定する。
     * @param headerName ヘッダー名
     * @param value ヘッダーの値
     */
    @Published
    public void setHeader(String headerName, String value) {
        headers.put(headerName, value);
    }

    /** HTTPレスポンスヘッダを格納するMap */
    private Map<String, String> headers = null;


    /**
     * Content-Typeの値を取得する。
     * <p/>
     * Content-Typeが設定されている場合は、以下のソースコードと等価である。
     * <code><pre>
     *   this.headers().get("Content-Type")
     * </pre></code>
     * <p/>
     *
     * Content-Typeが設定されていない場合は、以下の処理を行う。<br />
     * ・{@link WebConfig#getAddDefaultContentTypeForNoBodyResponse()} がtrueの場合、
     * またはボディが存在する場合に"text/plain;charset=UTF-8"を設定する。<br />
     *
     * @return Contents-Typeの値
     */
    @Published
    public String getContentType() {
        String contentType = headers.get("Content-Type");
        if (contentType == null && needsDefaultContentType()) {
            headers.put("Content-Type", "text/plain;charset=UTF-8");
        }
        return headers.get("Content-Type");
    }

    /**
     * Content-Type設定されていない場合に、デフォルトのContent-Typeを付与するべきか否かを判定する。
     *
     * @return デフォルトのContent-Typeを付与すべき時はtrue。
     */
    private boolean needsDefaultContentType() {
        return WebConfigFinder.getWebConfig().getAddDefaultContentTypeForNoBodyResponse() || !isBodyEmpty();
    }

    /**
     * Content-Typeに指定された文字エンコーディングを取得する。
     * @return 文字エンコーディング
     */
    public Charset getCharset() {
        if (charset == null) {
            String contentType = getContentType();
            if (contentType != null) {
                Matcher mt = CHARSET_ATTR_IN_CONTENT_PATH.matcher(contentType);
                if (mt.matches()) {
                    charset = Charset.forName(mt.group(1));
                }
            }
            if (charset == null) {
                charset = UTF_8;
            }
        }
        return charset;
    }
    /** 文字エンコーディング */
    private Charset charset = null;

    /**
     * Content-Typeを設定する。
     * <p/>
     * Content-Typeのデフォルト値は、"text/plain;charset=UTF-8" である。<br/>
     * ボディに書き込む内容をコンテンツパスで指定する場合、
     * Content-Typeはコンテンツパスの拡張子から自動的に決定される為、
     * このメソッドを明示的に使用する必要は無い。
     *
     * @param contentType Content-Typeの値
     * @return 本オブジェクト
     * @see #getContentType()
     */
    @Published
    public HttpResponse setContentType(String contentType) {
        getHeaderMap().put("Content-Type", contentType);
        charset = null;
        return this;
    }

    /**
     * Locationの値を取得する。
     * <p/>
     * このメソッドの処理は以下のソースコードと等価である。
     * <code><pre>
     *   this.headers().get("Location")
     * </pre></code>
     *
     * @return Locationの値
     */
    @Published
    public String getLocation() {
        return this.headers.get("Location");
    }

    /**
     * Locationの値を設定する。
     * <p/>
     * リダイレクト時のHTTPクライアントの遷移先URIを設定する。<br/>
     * デフォルトでは設定されない。
     *
     * @param location 遷移先URI
     * @return 本オブジェクト
     * @see Status#SEE_OTHER
     */
    @Published
    public HttpResponse setLocation(String location) {
        this.headers.put("Location", location);
        return this;
    }

    /**
     * Content-Dispositionヘッダ。
     */
    protected static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * Content-Dispositionの値を設定する。
     * <p/>
     * Content-Typeが明示的に設定されていない場合、
     * 設定されたファイル名の拡張子に応じたContent-Typeを自動的に設定する。<br/>
     * 本メソッドではattachment属性を指定するため、ダウンロード時にダイアログが必ず表示される。
     *
     * @param fileName ファイル名
     * @return 本オブジェクト
     */
    @Published
    public HttpResponse setContentDisposition(String fileName) {
        setContentDisposition(fileName, false);
        return this;
    }

    /**
     * Content-Dispositionの値を設定する。
     * <p/>
     * Content-Typeが明示的に設定されていない場合、
     * 設定されたファイル名の拡張子に応じたContent-Typeを自動的に設定する。<br/>
     * {@code inline}に{@code true}を指定した場合、ダウンロードされたファイルは
     * クライアントアプリで自動的に開かれる。<br/>
     * ただし、実際にそのような挙動となるかどうかは、クライアントの設定
     * およびOSのセキュリティ設定に依存する。
     *
     * @param fileName ファイル名
     * @param inline インライン表示する場合は{@code true}
     * @return 本オブジェクト
     */
    @Published
    public HttpResponse setContentDisposition(String fileName, boolean inline) {
        if (headers.get("Content-Type") == null) {
            headers.put("Content-Type", MAGIC.getContentType(fileName));
        }
        setHeader(CONTENT_DISPOSITION,
                (inline ? "inline" : "attachment")
                        + (fileName == null ? "" : "; filename=\"" + fileName + "\"")
        );
        return this;
    }

    /**
     * Content-Dispositionの値を取得する。
     * @return Content-Dispositionの値
     */
    @Published(tag = "architect")
    public String getContentDisposition() {
        return headers.get(CONTENT_DISPOSITION);
    }

    /**
     * Transfer-Encodingの値を取得する。
     * <p/>
     * このメソッドの処理は以下のソースコードと等価である。
     * <pre>
     *   this.headers().get("Transfer-Encoding")
     * </pre>
     *
     * @return Transfer-Encodingの値
     */
    public String getTransferEncoding() {
        return headers.get("Transfer-Encoding");
    }

    /**
     * Transfer-Encodingの値を設定する。
     * <p/>
     * このヘッダの値が"chunked"であった場合、
     * コンテンツボディはchunked-encodingに従って読み書きされる。<br/>
     * デフォルトではこのヘッダは設定されない。
     *
     * @param encoding Transfer-Encodingの値
     * @return 本オブジェクト
     * @see Status#SEE_OTHER
     */
    public HttpResponse setTransferEncoding(String encoding) {
        headers.put("Transfer-Encoding", encoding);
        return this;
    }

    /**
     * サーバ側から送信されたクッキー情報のうち先頭のクッキーをを取得する。
     * @return サーバ側から送信されたクッキー情報のうち先頭のクッキー。クッキーが存在しない場合は<code>null</code>
     * @deprecated 本メソッドは、複数のクッキー情報のうち先頭のクッキーを返すことしかできません。
     *              複数のクッキー情報を返すことができる{@link #getCookieList()}を使用してください。
     */
    @Published
    @Deprecated
    public HttpCookie getCookie() {
        if (cookies.isEmpty()) {
            return null;
        }
        final Cookie servletCookie = cookies.get(0);
        final HttpCookie cookie = new HttpCookie();
        cookie.put(servletCookie.getName(), servletCookie.getValue());
        return cookie;
    }

    /**
     * サーバ側から送信されたクッキー情報のリストを取得する。
     * @return クッキー情報のリスト
     */
    public List<Cookie> getCookieList() {
        return cookies;
    }

    /**
     * サーバ側から送信されたクッキーのリストを{@link HttpCookie}として取得する。
     * {@link HttpCookie}は同じ属性を持つ複数のクッキーを保持する仕様であるため、
     * クッキーの属性が各々異なることを考慮し、リストとして返却する。
     * @return クッキー ({@link HttpCookie})のリスト
     */
    @Published
    public List<HttpCookie> getHttpCookies() {
        List<HttpCookie> httpCookies = new ArrayList<HttpCookie>();

        for(Cookie servletCookie: cookies) {
            httpCookies.add(HttpCookie.fromServletCookie(servletCookie));
        }

        return httpCookies;
    }

    /**
     * サーバ側から送信されたクッキー情報を設定する。
     * @param cookie クッキー情報オブジェクト
     * @return 本オブジェクト
     * @deprecated 本メソッドは、複数のクッキー情報を設定することを意図したメソッド名を持つ
     *             {@link #addCookie(HttpCookie)}に置き換わりました。
     */
    @Published
    @Deprecated
    public HttpResponse setCookie(HttpCookie cookie) {
        return addCookie(cookie);
    }

    /**
     * サーバ側から送信されたクッキー情報を設定する。
     * @param cookie クッキー情報オブジェクト
     * @return 本オブジェクト
     */
    @Published
    public HttpResponse addCookie(HttpCookie cookie) {
        cookies.addAll(cookie.convertServletCookies());
        return this;
    }

    /**
     * Cookie文字列を格納する{@link List}
     */
    private final List<Cookie> cookies;

    /**
     * コンテンツパスを設定する。
     * <p/>
     * 本処理は{@link #setContentPath(ResourceLocator)}に委譲する。
     *
     * @param path コンテンツパス
     * @return 本オブジェクト
     */
    @Published
    public HttpResponse setContentPath(String path) {
        setContentPath(ResourceLocator.valueOf(path));
        return this;
    }

    /**
     * コンテンツパスを設定する。
     * <p/>
     * 指定した{@link ResourceLocator}オブジェクトが{@code null}でない場合は、
     * リソース名からContent-Typeを自動的に設定した後、コンテンツパスを設定する。<br/>
     * {@code ResourceLocator}オブジェクトが{@code null}の場合は、コンテンツパスのみ設定する。
     *
     * @param resource コンテンツパス
     * @return 本オブジェクト
     * @see #setContentPath(String)
     */
    @Published
    public HttpResponse setContentPath(ResourceLocator resource) {
        if (resource != null) {
            setContentType(MAGIC.getContentType(resource.getResourceName()));
        }
        body.setContentPath(resource);
        return this;
    }

    /** ファイル識別子からコンテンツタイプを判定する。 */
    static final MimetypesFileTypeMap MAGIC = new MimetypesFileTypeMap();

    static {
        MAGIC.addMimeTypes("text/css css");
        MAGIC.addMimeTypes("text/plain txt");
        MAGIC.addMimeTypes("text/plain text");
        MAGIC.addMimeTypes("application/excel xls");
        MAGIC.addMimeTypes("application/mspowerpoint ppt");
        MAGIC.addMimeTypes("application/msword doc");
        MAGIC.addMimeTypes("application/pdf pdf");
        MAGIC.addMimeTypes("application/zip zip");
        MAGIC.addMimeTypes("image/jpeg jpg");
        MAGIC.addMimeTypes("image/png png");
        MAGIC.addMimeTypes("image/gif gif");
    }

    /**
     * コンテンツパスを取得する。
     * <p/>
     * HTTPレスポンスボディに書き込むコンテンツパスを取得する。
     *
     * @return コンテンツパス
     */
    @Published
    public ResourceLocator getContentPath() {
        return body.getContentPath();
    }

    /**
     * Content-Lengthの値を取得する。
     * <p/>
     * HTTPレスポンスボディの内容がこのオブジェクト自体に保持されている場合に限り、
     * そのバイト数を返す。<br/>
     * それ以外は{@code null}を返す。
     *
     * @return Content-Lengthの値
     */
    @Published
    public String getContentLength() {
        Long length = body.length();
        if (length == null) {
            return null;
        }
        String val = String.valueOf(length);
        headers.put("Content-Length", val);
        return val;
    }

    /**
     * リソースを開放する。
     * @return 本オブジェクト
     */
    public HttpResponse cleanup() {
        ResponseBody.cleanup();
        return this;
    }

    /**
     * HTTPレスポンスのボディ内容を格納するオブジェクト。
     */
    private ResponseBody body = new ResponseBody(this);

    /**
     * HTTPレスポンスボディの内容が設定されていなければ{@code true}を返す。
     * @return ボディの内容が設定されていなければ{@code true}
     */
    @Published(tag = "architect")
    public boolean isBodyEmpty() {
        return body.isEmpty();
    }

    /**
     * HTTPレスポンスボディの内容を表す文字列を返す。
     * @return ボディの内容を表す文字列を返す
     */
    @Published(tag = "architect")
    public String getBodyString() {
        return body.toString();
    }

    /**
     * HTTPレスポンスボディの内容を保持するストリームを取得する。
     * @return HTTPレスポンスボディの内容を保持するストリーム
     */
    @Published(tag = "architect")
    public InputStream getBodyStream() {
        return body.getInputStream();
    }

    /**
     * HTTPレスポンスボディの内容を保持するストリームを設定する。
     * @param bodyStream HTTPレスポンスボディの内容を保持するストリーム
     * @return 本オブジェクト
     */
    @Published(tag = "architect")
    public HttpResponse setBodyStream(InputStream bodyStream) {
        body.setInputStream(bodyStream);
        return this;
    }

    /**
     * HTTPレスポンスボディに文字列を書き込む。
     * <p/>
     * このメソッドで書き込まれたデータは、本オブジェクトが保持する
     * バッファに保持され、クライアントソケットに対する書き込みは一切発生しない。
     * (このライタに対するflush()は単に無視される。)<br/>
     * 実際にソケットに対するレスポンス処理が行われるのは、
     * {@link nablarch.fw.web.handler.HttpResponseHandler}にレスポンスオブジェクトが戻された後である。
     * また、このオブジェクトにコンテンツパスが設定されている場合、
     * このライタに書き込まれた内容は単に無視される。
     *
     * @param text 書き込む文字列
     * @return 本オブジェクト
     * @throws HttpErrorResponse
     *     バッファの上限を越えてデータが書き込まれた場合
     */
    @Published
    public HttpResponse write(CharSequence text) throws HttpErrorResponse {
        body.write(text);
        return this;
    }

    /**
     * HTTPレスポンスボディにバイト配列を書き込む。
     * <p/>
     * このメソッドで書き込まれたデータは、本オブジェクトが保持する
     * バッファに保持され、クライアントソケットに対する書き込みは一切発生しない。
     * (このライタに対するflush()は単に無視される。)<br/>
     * 実際にソケットに対するレスポンス処理が行われるのは、
     * {@link nablarch.fw.web.handler.HttpResponseHandler}にレスポンスオブジェクトが戻された後である。
     * また、このオブジェクトにコンテンツパスが設定されている場合、
     * このライタに書き込まれた内容は単に無視される。
     *
     * @param bytes 書き込むバイト配列
     * @return 本オブジェクト
     * @throws HttpErrorResponse
     *     バッファの上限を越えてデータが書き込まれた場合
     */
    @Published
    public HttpResponse write(byte[] bytes) throws HttpErrorResponse {
        body.write(bytes);
        return this;
    }


    /**
     * HTTPレスポンスボディにバイト配列を書き込む。
     * <p/>
     * このメソッドで書き込まれたデータは、本オブジェクトが保持する
     * バッファに保持され、クライアントソケットに対する書き込みは一切発生しない。
     * (このライタに対するflush()は単に無視される。)<br/>
     * 実際にソケットに対するレスポンス処理が行われるのは、
     * {@link nablarch.fw.web.handler.HttpResponseHandler}にレスポンスオブジェクトが戻された後である。
     * また、このオブジェクトにコンテンツパスが設定されている場合、
     * このライタに書き込まれた内容は単に無視される。
     *
     * @param bytes 書き込むバイト列を格納したバッファ
     * @return 本オブジェクト
     * @throws HttpErrorResponse
     *     バッファの上限を越えてデータが書き込まれた場合
     */
    @Published
    public HttpResponse write(ByteBuffer bytes) throws HttpErrorResponse {
        body.write(bytes);
        return this;
    }

    /**
     * オブジェクトの内容と等価なHTTPレスポンスメッセージを返す。
     */
    public String toString() {
        String statusLine = String.format(
            "%s %s %s", getHttpVersion(), getStatusCode(), getReasonPhrase()
        );
        StringBuilder buffer = new StringBuilder(statusLine).append(LS);

        Iterator<Entry<String, String>>
            entries = getHeaderMap().entrySet().iterator();

        while (entries.hasNext()) {
            Entry<String, String> header = entries.next();
            if (header.getKey().equals("Transfer-Encoding")) {
                continue;
            }
            buffer.append(header.getKey())
                  .append(": ")
                  .append(header.getValue());
            if (entries.hasNext()) {
                buffer.append(LS);
            }
        }
        buffer.append(LS + LS)
              .append(body.toString());
        return buffer.toString();
    }

    /**
     * HTTPレスポンスメッセージを読み込んでHttpResponseオブジェクトを生成する。
     *
     * @param source HTTPレスポンスメッセージ
     */
    private void parseMessage(Reader source) {
        Scanner responseMessage = new Scanner(source);
        Scanner statusLine = new Scanner(responseMessage.nextLine());
        scanHttpVersion(statusLine);
        scanHttpStatus(statusLine);

        String header = null;
        while (responseMessage.hasNextLine()) {
            String line = responseMessage.nextLine();
            if (line.length() == 0) {
                break; // Blank line. following lines are message body.
            }
            if (header == null) {
                header = line;
                continue;
            }
            if (line.matches("\\s+.*")) {
                header += (" " + line.trim());
                continue;
            }
            scanHttpResponseHeader(header);
            header = line;
        }
        if (header != null) {
            scanHttpResponseHeader(header);
        }

        String transferEncoding = getTransferEncoding();
        if (transferEncoding != null && transferEncoding.equals("chunked")) {
            scanChunkedBody(responseMessage);
        } else {
            scanResponseBody(responseMessage);
        }
    }

    /**
     * HTTPレスポンスボディの内容を読み込む。
     *
     * @param message HTTPレスポンスメッセージ
     */
    private void scanResponseBody(Scanner message) {
        message.useDelimiter("\\r\\n");
        StringBuilder buffer = new StringBuilder();
        while (message.hasNext()) {
            String line = message.next();
            if (line == null) {
                break;
            }
            buffer.append(line);
            if (message.hasNext()) {
                buffer.append("\r\n");
            }
        }
        if (wasBytes) {
            write(getBytes(buffer, ASCII));
        } else {
            this.write(buffer);
        }
    }

    /**
     * 文字シーケンスをバイト配列に変換し、取得する。
     * @param chars 文字シーケンス
     * @param charset 文字シーケンスの文字コード
     * @return 文字シーケンスを変換したバイト配列
     */
    private byte[] getBytes(CharSequence chars, Charset charset) {
        ByteBuffer byteBuffer = charset.encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    /**
     * "chunked" Transfer-Encodingによる
     * HTTPレスポンスボディの内容を読み込む。
     * @param message HTTPレスポンスメッセージ
     */
    private void scanChunkedBody(Scanner message) {
        long chunkSize = -1;
        StringBuilder buffer = new StringBuilder();
        while (message.hasNextLong(16)) {
            chunkSize = message.nextLong(16);
            if (chunkSize <= 0) {
                break;
            }
            long readSize = 0;

            try {
                message.useDelimiter("\\r\\n");
                while (message.hasNext()) {
                    String chunk = message.next();
                    if (chunk == null) {
                        break;
                    }
                    readSize += (getBytes(chunk, ASCII).length + 2);
                    buffer.append(chunk);
                    if (readSize == chunkSize + 2) {
                        break;
                    } else if (readSize < chunkSize + 2) {
                        buffer.append("\r\n");
                    } else {
                        throw new RuntimeException("malformed chunk.: " + chunk);
                    }
                }
            } finally {
                // restores default delim.
                message.useDelimiter("\\p{javaWhitespace}+");
            }
        }
        write(ASCII.encode(buffer.toString()));
    }

    /**
     * HTTPレスポンスヘッダの内容を読み込む。
     *
     * @param header HTTPレスポンスメッセージ
     */
    private void scanHttpResponseHeader(String header) {
        Matcher m = HTTP_HEADER_SYNTAX.matcher(header);
        if (!m.matches()) {
            parseError(header);
        }
        if ("Set-Cookie".equalsIgnoreCase(m.group(1))) {
            this.addCookie(HttpCookie.fromSetCookieHeader(header));
        }
        this.headers.put(m.group(1), m.group(2));
    }

    /** HTTPヘッダの書式 */
    private static final Pattern HTTP_HEADER_SYNTAX = Pattern.compile(
        "([a-zA-Z0-9\\-]+):\\s(.*)", Pattern.DOTALL
    );

    /**
     * HTTPバージョンを読み込む。
     *
     * @param scanner HTTPレスポンスメッセージ
     */
    private void scanHttpVersion(Scanner scanner) {
        this.httpVersion = scanner.next(HTTP_VERSION_SYNTAX);
    }

    /**
     * HTTPレスポンスステータスを読み込む。
     *
     * @param scanner HTTPレスポンスメッセージ
     */
    private void scanHttpStatus(Scanner scanner) {
        String statusCode = scanner.next(HTTP_STATUS_CODE_SYNTAX);
        this.status = Status.valueOfCode(Integer.valueOf(statusCode));
    }

    /** HTTPステータスコードの書式 */
    private static final Pattern HTTP_STATUS_CODE_SYNTAX = Pattern.compile(
        "[1-5]\\d{2}"
    );

    /**
     * パース処理中のエラーを送出する。
     *
     * @param obj エラー情報オブジェクト
     */
    private void parseError(Object obj) {
        throw new RuntimeException(
        "Invalid http request message.: " + obj.toString()
        );
    }

    /**
     * 処理が正常終了したかどうかを返す。
     * <p/>
     * HTTPステータスコードが400未満であれば正常終了とみなす。
     */
    public boolean isSuccess() {
        return (getStatusCode() < 400);
    }
}
