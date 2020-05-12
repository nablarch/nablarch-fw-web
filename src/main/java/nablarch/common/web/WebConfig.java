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

    /** エラーメッセージをリクエストスコープに格納する際に使用するキー */
    private String errorMessageRequestAttributeName = "errors";

    /** CSRFトークンをHTTPリクエストヘッダーから取得する際に使用する名前 */
    private String csrfTokenHeaderName = "X-CSRF-TOKEN";

    /** CSRFトークンをHTTPリクエストパラメーターから取得する際に使用する名前 */
    private String csrfTokenParameterName = "csrf-token";

    /** CSRFトークンをセッションスコープに格納する際に使用するキー */
    private String csrfTokenSessionStoredVarName = ExecutionContext.FW_PREFIX + "csrf-token";

    /** CSRFトークンを保存するセッションストアの名前 */
    private String csrfTokenSavedStoreName;

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

    /**
     * エラーメッセージをリクエストスコープに格納する際に使用するキーを取得する。
     *
     * @return エラーメッセージをリクエストスコープに格納する際に使用するキー
     */
    public String getErrorMessageRequestAttributeName() {
        return errorMessageRequestAttributeName;
    }

    /**
     * エラーメッセージをリクエストスコープに格納する際に使用するキーを設定する。
     *
     * @param errorMessageRequestAttributeName エラーメッセージをリクエストスコープに格納する際に使用するキー
     */
    public void setErrorMessageRequestAttributeName(final String errorMessageRequestAttributeName) {
        this.errorMessageRequestAttributeName = errorMessageRequestAttributeName;
    }

    /**
     * CSRFトークンをHTTPリクエストヘッダーから取得する際に使用する名前を取得する。
     * 
     * @return CSRFトークンをHTTPリクエストヘッダーから取得する際に使用する名前
     */
    public String getCsrfTokenHeaderName() {
        return csrfTokenHeaderName;
    }

    /**
     * CSRFトークンをHTTPリクエストヘッダーから取得する際に使用する名前を設定する。
     * 
     * @param csrfTokenHeaderName CSRFトークンをHTTPリクエストヘッダーから取得する際に使用する名前
     */
    public void setCsrfTokenHeaderName(String csrfTokenHeaderName) {
        this.csrfTokenHeaderName = csrfTokenHeaderName;
    }

    /**
     * CSRFトークンをHTTPリクエストパラメーターから取得する際に使用する名前を取得する。
     * 
     * @return CSRFトークンをHTTPリクエストパラメーターから取得する際に使用する名前
     */
    public String getCsrfTokenParameterName() {
        return csrfTokenParameterName;
    }

    /**
     * CSRFトークンをHTTPリクエストパラメーターから取得する際に使用する名前を設定する。
     * 
     * @param csrfTokenParameterName CSRFトークンをHTTPリクエストパラメーターから取得する際に使用する名前
     */
    public void setCsrfTokenParameterName(String csrfTokenParameterName) {
        this.csrfTokenParameterName = csrfTokenParameterName;
    }

    /**
     * CSRFトークンをセッションスコープに格納する際に使用するキーを取得する。
     * 
     * @return CSRFトークンをセッションスコープに格納する際に使用するキー
     */
    public String getCsrfTokenSessionStoredVarName() {
        return csrfTokenSessionStoredVarName;
    }

    /**
     * CSRFトークンをセッションスコープに格納する際に使用するキーを設定する。
     * 
     * @param csrfTokenSessionStoredVarName CSRFトークンをセッションスコープに格納する際に使用するキー
     */
    public void setCsrfTokenSessionStoredVarName(String csrfTokenSessionStoredVarName) {
        this.csrfTokenSessionStoredVarName = csrfTokenSessionStoredVarName;
    }

    /**
     * CSRFトークンを保存するセッションストアの名前を取得する。
     * 
     * @return CSRFトークンを保存するセッションストアの名前
     */
    public String getCsrfTokenSavedStoreName() {
        return csrfTokenSavedStoreName;
    }

    /**
     * CSRFトークンを保存するセッションストアの名前を設定する。
     * 
     * @param csrfTokenSavedStoreName CSRFトークンを保存するセッションストアの名前
     */
    public void setCsrfTokenSavedStoreName(String csrfTokenSavedStoreName) {
        this.csrfTokenSavedStoreName = csrfTokenSavedStoreName;
    }
}
