package nablarch.fw.web.i18n;

/**
 * ファイル名を言語の切り替えに使用するクラス。
 * @author Kiyohito Itoh
 */
public class FilenameBasedResourcePathRule extends ResourcePathRule {
    /**
     * {@inheritDoc}
     * <pre>
     * 下記のパスを返す。
     * 
     *   pathFromContextRoot: "/aaa/bbb/ccc.css"
     *   language: "ja"
     *   
     *   戻り値: "/aaa/bbb/ccc_ja.css"
     * </pre>
     */
    @Override
    protected String createPathForLanguage(String pathFromContextRoot, String language) {
        int extensionIndex = pathFromContextRoot.lastIndexOf('.');
        String extension = pathFromContextRoot.substring(extensionIndex);
        String basePath = pathFromContextRoot.substring(0, extensionIndex);
        String languageSuffix = "_" + language + extension;
        return basePath + languageSuffix;
    }
}
