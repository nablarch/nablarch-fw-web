package nablarch.fw.web.i18n;

import java.net.MalformedURLException;
import java.util.Locale;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import nablarch.core.ThreadContext;
import nablarch.core.util.annotation.Published;
import nablarch.fw.web.servlet.UriUtil;

/**
 * 言語対応リソースパスのルールを表すクラス。
 * <p/>
 * 自身が表すルールに基づき言語対応のリソースパスを提供する。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class ResourcePathRule {

    /** {@link ServletContextCreator}のインスタンス */
    private ServletContextCreator servletContextCreator = new BasicServletContextCreator();
    
    /**
     * {@link ServletContextCreator}を設定する。
     * 
     * @param servletContextCreator サーブレットコンテキスト生成クラス
     */
    public void setServletContextCreator(ServletContextCreator servletContextCreator) {
    	this.servletContextCreator = servletContextCreator;
    }

    /**
     * 言語対応のリソースパスを取得する。
     * <pre>
     * 言語は{@link ThreadContext#getLanguage()}から取得する。
     * {@link ThreadContext#getLanguage()}から言語を取得できない場合は指定されたリソースパスをそのまま返す。
     *
     * 言語対応のリソースパスが指すファイルが存在する場合は言語対応のリソースパスを返し、
     * 存在しない場合は指定されたリソースパスをそのまま返す。
     * 指定されたリソースパスに拡張子を含まない場合は指定されたリソースパスをそのまま返す。
     *
     * 言語対応のリソースパスは{@link #createPathForLanguage(String, String)}メソッドを呼び出し作成する。
     * {@link #createPathForLanguage(String, String)}メソッドはサブクラスにより実装される。
     * <pre>
     * @param path オリジナルのリソースパス
     * @param request リクエスト
     * @return 言語対応のリソースパス
     */
    public String getPathForLanguage(String path, HttpServletRequest request) {

        Locale locale = ThreadContext.getLanguage();
        if (locale == null) { // 言語がnullの場合。
            return path;
        }

        // リソースパスの判定にクエリパラメータは不要なため除去
        // （クエリパラメータがあると正しい存在チェックができず、クエリパラメータに含まれる
        //   文字列が原因でエラーになる可能性もあるため、クエリパラメータは除去する）
        String basePath = removeQueryParameter(path);

        int extensionIndex = basePath.lastIndexOf('.');
        if (extensionIndex == -1) { // 拡張子を含まない場合。
            return path;
        }

        // コンテキストルートからのパスに変換。
        String pathFromContextRoot = convertToPathFromContextRoot(basePath, request);

        // 言語対応のリソースパスの作成。
        String pathForLanguage = createPathForLanguage(pathFromContextRoot, locale.getLanguage());

        // 言語対応のリソースパスが指すファイルが存在しない場合。
        if (!existsResource(pathForLanguage, request)) {
            return path;
        }

        // コンテキストルートからのパス変換時に追加されたパスがある場合は取り除く。
        String addedPath = pathFromContextRoot.replace(basePath, "");
        if (!addedPath.isEmpty()) {
            pathForLanguage = pathForLanguage.substring(addedPath.length());
        }

        // 変換後のリソースパスにクエリパラメータを戻して返却
        return pathForLanguage + path.replace(basePath, "");
    }

    /**
     * 指定されたパスが指すファイルが存在するか否かを判定する。
     * @param request リクエスト
     * @param resourcePath リソースパス
     * @return ファイルが存在する場合はtrue
     */
    protected boolean existsResource(String resourcePath, HttpServletRequest request) {
        try {
            return getServletContext(request).getResource(resourcePath)
                        != null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * サーブレットコンテキストを取得する。
     *
     * @param request リクエスト
     * @return サーブレットコンテキスト
     */
    private ServletContext getServletContext(HttpServletRequest request) {
        return servletContextCreator.create(request);
    }
    
    /**
     * コンテキストルートからのパスに変換する。
     * @param path パス
     * @param request リクエスト
     * @return コンテキストルートからのパス
     */
    protected String convertToPathFromContextRoot(String path, HttpServletRequest request) {
        return UriUtil.convertToPathFromContextRoot(path, request);
    }

    /**
     * 言語対応のリソースパスを作成する。
     * @param pathFromContextRoot コンテキストルートからのパス
     * @param language 言語
     * @return 言語対応のリソースパス
     */
    protected abstract String createPathForLanguage(String pathFromContextRoot, String language);

    /**
     * リソースパスからクエリパラメータ部分を除去する。
     *
     * @param path リソースパス
     * @return クエリパラメータ部分を除去したリソースパス
     */
    private String removeQueryParameter(String path) {
        int queryParameterIndex = path.indexOf('?');
        if (queryParameterIndex == -1) {
            return path;
        }
        return path.substring(0, queryParameterIndex);
    }
}
