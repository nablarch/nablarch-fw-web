package nablarch.common.web;

import nablarch.fw.ExecutionContext;

/**
 * Webアプリケーションの設定を保持するクラス。
 * @author Taichi Uragami
 *
 */
public class WebConfig {

    /** 二重サブミット防止トークンをHTMLに埋め込む際にinput要素のname属性に設定する名前 */
    private String doubleSubmissionTokenParameterName = ExecutionContext.FW_PREFIX + "token";

    /** 二重サブミット防止トークンをリクエストスコープに格納する際に使用するキー */
    private String doubleSubmissionTokenRequestAttributeName = ExecutionContext.FW_PREFIX
            + "request_token";

    /** 二重サブミット防止トークンをセッションスコープに格納する際に使用するキー */
    private String doubleSubmissionTokenSessionAttributeName = "/" + ExecutionContext.FW_PREFIX
            + "session_token";

    /**
     * 二重サブミット防止トークンをHTMLに埋め込む際にinput要素のname属性に設定する名前を取得する。
     * @return 二重サブミット防止トークンをHTMLに埋め込む際にinput要素のname属性に設定する名前
     */
    public String getDoubleSubmissionTokenParameterName() {
        return doubleSubmissionTokenParameterName;
    }

    /**
     * 二重サブミット防止トークンをHTMLに埋め込む際にinput要素のname属性に設定する名前を設定する。
     * @param doubleSubmissionTokenParameterName 二重サブミット防止トークンをHTMLに埋め込む際にinput要素のname属性に設定する名前
     */
    public void setDoubleSubmissionTokenParameterName(String doubleSubmissionTokenParameterName) {
        this.doubleSubmissionTokenParameterName = doubleSubmissionTokenParameterName;
    }

    /**
     * 二重サブミット防止トークンをリクエストスコープに格納する際に使用するキーを取得する。
     * @return 二重サブミット防止トークンをリクエストスコープに格納する際に使用するキー
     */
    public String getDoubleSubmissionTokenRequestAttributeName() {
        return doubleSubmissionTokenRequestAttributeName;
    }

    /**
     * 二重サブミット防止トークンをリクエストスコープに格納する際に使用するキーを設定する。
     * @param doubleSubmissionTokenRequestAttributeName 二重サブミット防止トークンをリクエストスコープに格納する際に使用するキー
     */
    public void setDoubleSubmissionTokenRequestAttributeName(
            String doubleSubmissionTokenRequestAttributeName) {
        this.doubleSubmissionTokenRequestAttributeName = doubleSubmissionTokenRequestAttributeName;
    }

    /**
     * 二重サブミット防止トークンをセッションスコープに格納する際に使用するキーを取得する。
     * @return 二重サブミット防止トークンをセッションスコープに格納する際に使用するキー
     */
    public String getDoubleSubmissionTokenSessionAttributeName() {
        return doubleSubmissionTokenSessionAttributeName;
    }

    /**
     * 二重サブミット防止トークンをセッションスコープに格納する際に使用するキーを設定する。
     * @param doubleSubmissionTokenSessionAttributeName 二重サブミット防止トークンをセッションスコープに格納する際に使用するキー
     */
    public void setDoubleSubmissionTokenSessionAttributeName(
            String doubleSubmissionTokenSessionAttributeName) {
        this.doubleSubmissionTokenSessionAttributeName = doubleSubmissionTokenSessionAttributeName;
    }
}
