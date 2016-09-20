package nablarch.fw.web.handler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Request;
import nablarch.fw.handler.JavaPackageMappingEntry;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpAccessLogFormatter.HttpAccessLogContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * このハンドラは、画面オンライン処理におけるリクエストパス中の部分文字列(ベースURI)を
 * Javaパッケージ階層にマッピングすることで、動的に委譲先ハンドラを決定するディスパッチ処理を行う。
 * 本ハンドラの実装は基本的に {@link RequestPathJavaPackageMapping} のラッパーであり
 * その機能は以下の2点を除けば全く同じものである。
 * <pre>
 * 1. ディスパッチ対象のクラスが確定した時点で、HTTPアクセスログにその内容を出力する。
 * 2. ベースパスを設定する際にURLの書式バリデーションを行うアクセサ {@link #setBaseUri(String) }を追加。
 * </pre>
 * 
 * 機能の詳細については、 {@link RequestPathJavaPackageMapping} を参照すること。
 * 
 * @see    RequestPathJavaPackageMapping
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class HttpRequestJavaPackageMapping implements HttpRequestHandler {
    
    /** {@inheritDoc}
     * URI中の部分文字列をJavaパッケージへマッピングすることで動的に
     * 委譲先のハンドラを決定し、処理を委譲する。
     * また、委譲先のクラスがハンドラインターフェースを実装していない場合でも、
     * {@link nablarch.fw.web.HttpMethodBinding} により処理を委譲する。
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        return (HttpResponse) mapping.handle(request, context);
    }

    /**
     * コンストラクタ
     */
    public HttpRequestJavaPackageMapping() {
        mapping = new Mapping();
    }

    /**
     * コンストラクタ
     * @param baseUri     マップ元リクエストURI
     * @param basePackage マップ先Javaパッケージ
     */
    public HttpRequestJavaPackageMapping(String baseUri, String basePackage) {
        mapping = new Mapping(baseUri, basePackage);
    }
    
    /** ディスパッチャの実体 */
    private final Mapping mapping;
    
    /**
     * ディスパッチャの実体。
     * note:
     *   共通ハンドラを直接継承すると、HttpRequestHandlerを実装することが
     *   できないので、内部クラスに委譲する形にしている。
     */
    public static class Mapping extends nablarch.fw.handler.RequestPathJavaPackageMapping {
        /** コンストラクタ */
        public Mapping() {
            super();
        }

        /**
         * コンストラクタ
         * @param basePath    ベースパス
         * @param basePackage ベースパッケージ
         */
        public Mapping(String basePath, String basePackage) {
            super(basePath, basePackage);
        }

        /** {@inheritDoc} */
        protected void writeDispatchingClassLog(Request<?> req, ExecutionContext ctx, String fqn) {
            if (ctx instanceof ServletExecutionContext) {
                ServletExecutionContext context = (ServletExecutionContext) ctx;
                HttpAccessLogContext logContext = HttpAccessLogUtil.getAccessLogContext(req, context);
                logContext.setDispatchingClass(fqn);
                HttpAccessLogUtil.logDispatchingClass(logContext);
            }
        }
    }

    /**
     * ベースパスを設定する。
     * @param basePath ベースパス
     * @return このオブジェクト自体
     */
    public HttpRequestJavaPackageMapping setBasePath(String basePath) {
        return setBaseUri(basePath);
    }
    
    /**
     * ベースURIを設定する。({@link #setBasePath(String)}のシノニム)
     * @param baseUri ベースURI
     * @return このオブジェクト自体
     */
    public HttpRequestJavaPackageMapping setBaseUri(String baseUri) {
        try {
            if (baseUri != null) {
                baseUri = new URI(baseUri).toASCIIString();
            }
            mapping.setBasePath(baseUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid requestPath: " + baseUri, e);
        }
        return this;
    }

    /**
     * ベースパッケージを設定する。
     * @param basePackage ベースパッケージ
     * @return このオブジェクト自体
     */
    public HttpRequestJavaPackageMapping setBasePackage(String basePackage) {
        mapping.setBasePackage(basePackage);
        return this;
    }
    
    /**
     * RequestHandlerEntryでURIに合致したマッピング先Javaパッケージを上書きする場合に使用する、JavaPackageMappingEntryのリストを設定する。
     * 
     * @param optionalPackageMappingEntries RequestHandlerEntryでURIに合致したマッピング先Javaパッケージを上書きする場合に使用する、JavaPackageMappingEntryのリスト
     * @return このオブジェクト自体
     */
    public HttpRequestJavaPackageMapping setOptionalPackageMappingEntries(List<JavaPackageMappingEntry> optionalPackageMappingEntries) {
        mapping.setOptionalPackageMappingEntries(optionalPackageMappingEntries);
        return this;
    }
}
