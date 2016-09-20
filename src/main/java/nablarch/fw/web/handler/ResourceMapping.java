package nablarch.fw.web.handler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.ResourceLocator;

/**
 * リクエストURIに応じて、リソースファイルに対するマッピングを行うリクエストハンドラ。
 * <pre>
 * 画像等の静的ファイルへのリクエストやフォーワードを介さないJSP画面へのアクセスは、
 * 通常、Webサーバ・アプリケーションサーバでレスポンスを行う為、フレームワーク側で制御することができない。
 * このリクエストハンドラを経由させることで、これらのアクセスについても
 * 認可を始めとする共通処理の制御下におくことができる。
 * マッピング先のパスには、{@link HttpResponse#setContentPath(String)}の書式に従って、
 * 以下の3種類のいずれかを指定することができる。
 *   1. コンテキストクラスローダ上のリソース
 *   2. サーブレットフォーワード（JSPを含む)
 *   3. 内部フォーワードの実行結果
 * ただし、ファイルシステム上のローカルファイル(file://スキーム)は使用できない。
 * 
 * 次の例では、特定のベースURI(/webapp/resource/)下の画像ファイルに対するリクエストを、
 * サーブレットコンテキスト上のリソース(/WEB-INF/resource/)を参照するようにマッピングしている。
 * こうすることで、これらのファイルをフレームワークの認証・認可の制御下に置くことができる。
 *   new StaticResource("/admin/resource/", "servlet:///WEB-INF/resource/");
 * HTTPリクエストと、それに対するレスポンスのコンテンツパスとの対応は以下のようになる。
 * ===========================================================================
 *  HTTPリクエストライン                コンテンツパス [コンテンツタイプ]
 * ===========================================================================
 * GET /admin/resource/style.css  ->  servlet:///WEB-INF/resource/style.css 
 *                                    [text/css]
 * -------------------------------    --------------------------------------
 * GET /admin/resource/js/init.js ->  servlet:///WEB-INF/resource/js/init.js
 *                                    [application/javascript]
 * ===========================================================================
 * </pre>
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @see HttpResponse#setContentPath(String)
 */
public class ResourceMapping implements HttpRequestHandler {

    /**
     * 特定のbaseUri配下へのリクエストを静的ファイルにマッピングする
     * リクエストハンドラを生成する。
     *
     * @param baseUri マッピング元ベースURI
     * @param basePath マッピング先リソースパス
     */
    public ResourceMapping(String baseUri, String basePath) {
        setBaseUri(baseUri);
        setBasePath(basePath);
    }

    /**
     * デフォルトコンストラクタ。
     */
    public ResourceMapping() {
        baseUri  = null;
        basePath = null;
    }

    /**
     * マッピング元ベースURIを設定する。
     *
     * @param baseUri マッピング元ベースURI
     * @return このオブジェクト自体
     */
    public ResourceMapping setBaseUri(String baseUri) {
        try {
            this.baseUri = new URI(baseUri).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid requestPath: " + baseUri, e);
        }
        return this;
    }
    
    /**
     * マッピング先リソースパスを設定する。
     *
     * @param basePath マッピング先リソースパス
     * @return このオブジェクト自体
     */
    public ResourceMapping setBasePath(String basePath) {
        Matcher m = ResourceLocator.SYNTAX.matcher(basePath);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid path: " + basePath);
        }
        this.scheme = (m.group(1) != null) ? m.group(1)
                    : (m.group(2) != null) ? m.group(2)
                    : "servlet";
        
        if (scheme.equals("file")) {
            throw new IllegalArgumentException("Invalid scheme: " + scheme);
        }
        this.basePath = m.group(4);
        return this;
    }

    /** マッピング元ベースURI */
    private String baseUri;

    /** マッピング先リソーススキーム */
    private String scheme;

    /** マッピング先ベースパス */
    private String basePath;

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(ResourceMapping.class);

    /**
     * {@inheritDoc}
     * <pre>
     * このクラスの実装では以下の処理を行う。
     *   1. リクエストURI中のbaseUri以下の部分を取得する。
     *   2. docRootに1.の結果を連結した文字列をコンテンツタイプとする。
     *   3. リクエストURIの拡張子からコンテンツタイプを判定する。
     *   4. HttpResponseを生成し、2,3の結果をそれに設定する。
     *   5. 4の結果を返す。
     * </pre>
     *
     * @see HttpResponse#setContentPath(String)
     */
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        if (baseUri == null || basePath == null) {
            throw new IllegalStateException(
              "baseUri and basePath must be set."
            );
        }
        if (!req.getRequestUri().startsWith(baseUri)) {
            return HttpResponse.Status.NOT_FOUND.handle(req, ctx);
        }

        String relPath = req.getRequestPath()
                            .replaceFirst("^" + Pattern.quote(baseUri), "");
        String contentPath = scheme
                           + "://"
                           + (basePath + relPath).replaceAll("//+", "/");

        LOGGER.logInfo("this request mapped to the resource path of '" + contentPath + "'");
        ResourceLocator resource = ResourceLocator.valueOf(contentPath);
        if (!resource.exists()) {
            return HttpResponse.Status.NOT_FOUND.handle(req, ctx);
        }

        return new HttpResponse()
                .setStatusCode(200)
                .setContentPath(resource);
    }
}
