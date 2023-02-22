package nablarch.fw.web.upload;

import nablarch.fw.web.servlet.HttpRequestWrapper;

/**
 * マルチパート解析用のコンテキスト情報を保持するクラス。<br/>
 * {@link UploadSettings}が、アプリケーションスコープでアップロードに関する各種設定値を保持するのに対し、
 * 本クラスは、ファイルアップロード1リクエストの情報を保持する。
 *
 * @author T.Kawasaki
 */
class MultipartContext {

    /** リクエストのデフォルト文字エンコーディング */
    private static final String DEFAULT_REQUEST_CHARACTER_ENCODING = "UTF-8";

    /** Content-Type */
    private final String contentType;

    /** Content-Length */
    private final int contentLength;

    /** リクエストのエンコーディング */
    private final String requestCharacterEncoding;

    /**
     * コンストラクタ。
     *
     * @param wrapper {@link HttpRequestWrapper}
     */
    MultipartContext(HttpRequestWrapper wrapper) {
        this(wrapper.getContentType(),
             wrapper.getContentLength(),
             wrapper.getCharacterEncoding());
    }

    /**
     * コンストラクタ。
     *
     * @param contentType              Content-Type
     * @param contentLength            Content-Length
     * @param requestCharacterEncoding リクエストのエンコーディング
     */
    MultipartContext(String contentType, int contentLength, String requestCharacterEncoding) {
        this.contentLength = contentLength;
        this.requestCharacterEncoding = (requestCharacterEncoding == null)
                ? DEFAULT_REQUEST_CHARACTER_ENCODING
                : requestCharacterEncoding;
        this.contentType = contentType;
    }

    /**
     * HTTPリクエストのContent-Lengthを取得する。
     *
     * @return Content-Length
     */
    int getContentLength() {
        return contentLength;
    }

    /**
     * HTTPリクエストのエンコーディングを取得する。
     *
     * @return HTTPリクエストのエンコーディング
     * @see jakarta.servlet.http.HttpServletRequest#getCharacterEncoding()
     */
    String getRequestCharacterEncoding() {
        return requestCharacterEncoding;
    }

    /**
     * HTTPリクエストのContent-Typeを取得する。
     *
     * @return Content-Type
     */
    String getContentType() {
        return contentType;
    }
}
