package nablarch.fw.web;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.core.validation.Validatable;
import nablarch.fw.Request;
import nablarch.fw.web.upload.PartInfo;
import nablarch.fw.web.useragent.UserAgent;
import nablarch.fw.web.useragent.UserAgentParser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP/1.1(RFC2616)におけるリクエストメッセージのパーサ及び
 * その結果を格納するデータオブジェクト。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public abstract class HttpRequest implements Request<String[]>, Validatable<String[]> {

    /** デフォルトの{@link UserAgentParser}実装クラス */
    private static final UserAgentParser DEFAULT_USER_AGENT_PARSER = new UserAgentParser() {
        @Override
        public UserAgent parse(final String userAgentText) {
            return new UserAgent(userAgentText);
        }
    };

    /** HTTPリクエストURI */
    private String requestUri;

    /** マルチパート */
    private Map<String, List<PartInfo>> multipart = Collections.emptyMap();

    /**
     * HTTPリクエストメソッド名を返す。
     *
     * @return リクエストメソッド名
     */
    @Published
    public abstract String getMethod();

    /**
     * HTTPリクエストURIを返す。
     *
     * @return リクエストURI
     */
    @Published
    public String getRequestUri() {
        return requestUri;
    }

    /**
     * HTTPリクエストURIを設定する。
     *
     * @param requestUri リクエストURI
     * @return 本オブジェクト
     */
    public HttpRequest setRequestUri(final String requestUri) {
        this.requestUri = requestUri.trim();
        return this;
    }

    /**
     * HTTPリクエストURIのパス部分(クエリストリングを除いた部分)を返す。
     *
     * @return HTTPリクエストURIのパス部分
     */
    @Published
    public String getRequestPath() {
        return requestUri.replaceFirst("[?;].*$", "");
    }

    /**
     * リクエストパスを設定する。
     * <p/>
     * この実装では、リクエストURI中のリクエストパスを書き換える。
     *
     * @param requestPath リクエストパス
     * @return 本オブジェクト
     */
    @Published(tag = "architect")
    public HttpRequest setRequestPath(final String requestPath) {
        String dollarEscaped = requestPath.replace("$", "\\$");
        String uri = requestUri.replaceFirst("^(.+?)(\\?[^?]*)?$", dollarEscaped + "$2");
        setRequestUri(uri);
        return this;
    }

    /**
     * HTTPバージョン名を返す。
     *
     * @return HTTPバージョン名
     */
    @Published
    public abstract String getHttpVersion();

    /**
     * リクエストパラメータのMapを返す。
     * <pre>
     * HTTPリクエストメッセージ中の以下のパラメータを格納したMapを返す。
     *   1. リクエストURI中のクエリパラメータ
     *   2. メッセージボディ内のPOSTパラメータ
     * パラメータ名は重複する可能性があるので、値の型はString[]で定義されている。
     * </pre>
     *
     * @return リクエストパラメータのMap
     */
    public abstract Map<String, String[]> getParamMap();

    /**
     * リクエストパラメータを取得する。
     *
     * @param name パラメータ名
     * @return パラメータの値
     * @see #getParamMap()
     */
    public abstract String[] getParam(String name);

    /**
     * リクエストパラメータを設定する。
     *
     * @param name パラメータ名
     * @param params パラメータの値
     * @return 本オブジェクト
     */
    @Published
    public abstract HttpRequest setParam(String name, String... params);

    /**
     * リクエストパラメータを設定する。
     *
     * @param params リクエストパラメータのMap
     * @return 本オブジェクト
     */
    @Published(tag = "architect")
    public abstract HttpRequest setParamMap(Map<String, String[]> params);

    /**
     * HTTPリクエストヘッダを格納したMapを取得する。
     *
     * @return HTTPリクエストヘッダのMap
     */
    @Published
    public abstract Map<String, String> getHeaderMap();

    /**
     * HTTPリクエストヘッダの値を返す。
     *
     * @param headerName リクエストヘッダ名
     * @return HTTPリクエストヘッダの値
     */
    @Published
    public abstract String getHeader(String headerName);

    /**
     * HTTPリクエストのホストヘッダを取得する。
     *
     * @return ホストヘッダ
     */
    @Published
    public String getHost() {
        return getHeader("Host");
    }

    /**
     * 本リクエストで送信されるクッキー情報を取得する。
     *
     * @return クッキー情報オブジェクト
     */
    @Published(tag = "architect")
    public abstract HttpCookie getCookie();

    /**
     * マルチパートの一部を取得する。
     * <p/>
     * 引数で指定した名称に合致するパートが存在しない場合、空のリストが返却される。
     *
     * @param name 名称(inputタグのname属性)
     * @return マルチパート
     */
    @Published
    public List<PartInfo> getPart(final String name) {
        final List<PartInfo> list = multipart.get(name);
        return (list == null)
                ? Collections.<PartInfo>emptyList()
                : list;
    }

    /**
     * マルチパートを設定する。
     *
     * @param multipart マルチパート
     */
    @Published(tag = "architect")
    public void setMultipart(Map<String, List<PartInfo>> multipart) {
        if (multipart == null) {
            multipart = Collections.emptyMap();
        }
        this.multipart = multipart;
    }

    /**
     * 本HTTPリクエストの全マルチパートを取得する。
     * <p/>
     * 戻り値のMapの構造を以下に示す。
     * <dl>
     * <dt>キー</dt>
     * <dl>名称(inputタグのname属性)</dl>
     * <dt>値</dt>
     * <dl>キーのname属性でアップロードされたマルチパート</dl>
     * </dl>
     *
     * @return 全マルチパート
     * @see #getPart(String)
     */
    @Published
    public Map<String, List<PartInfo>> getMultipart() {
        return multipart;
    }
    
    /**
     * UserAgent情報を取得する。
     * <p/>
     * HTTPヘッダ("User-Agent")よりUser-Agent文字列を取得し、
     * {@link SystemRepository}に設定された{@link UserAgentParser}(コンポーネント名"userAgentParser")で解析を行う。
     * <br/>
     * パーサーが取得できない場合は、
     * 全ての項目にデフォルト値が設定された{@link UserAgent}オブジェクトが返却される。
     *
     * @param <UA> userAgentの型
     * @return UserAgentオブジェクト
     */
    @Published
    public <UA extends UserAgent> UA getUserAgent() {
        final String userAgentText = getHeader("User-Agent");
        final UserAgentParser parser = getUserAgentParser();
        return (UA) parser.parse(userAgentText);
    }

    /**
     * {@link UserAgentParser}実装クラスのインスタンスを取得する。
     *
     * @return {@link UserAgentParser}実装クラスのインスタンス
     */
    private UserAgentParser getUserAgentParser() {
        final UserAgentParser registered = SystemRepository.get("userAgentParser");
        return registered != null ? registered : DEFAULT_USER_AGENT_PARSER;
    }
}