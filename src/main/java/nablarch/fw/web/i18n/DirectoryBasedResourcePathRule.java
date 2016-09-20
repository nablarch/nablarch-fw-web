package nablarch.fw.web.i18n;

/**
 * コンテキストルート直下のディレクトリを言語の切り替えに使用するクラス。
 * @author Kiyohito Itoh
 */
public class DirectoryBasedResourcePathRule extends ResourcePathRule {
    /**
     * {@inheritDoc}
     * <pre>
     * 下記のパスを返す。
     * 
     *   pathFromContextRoot: "/aaa/bbb/ccc.css"
     *   language: "ja"
     *   
     *   戻り値: "/ja/aaa/bbb/ccc.css"
     * </pre>
     */
    @Override
    protected String createPathForLanguage(String pathFromContextRoot, String language) {
        return "/" + language + pathFromContextRoot;
    }
}
