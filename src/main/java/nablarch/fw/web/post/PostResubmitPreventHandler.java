package nablarch.fw.web.post;

import java.util.Collections;
import java.util.Map;

import nablarch.core.ThreadContext;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;

/**
 * POST再送信防止ハンドラ。
 * <p/>
 * 本ハンドラでは、POSTで受け付けたリクエストに対して、
 * リダイレクトを使用し、再度リクエストを受け付けることで、
 * ブラウザの戻るボタンによるPOST再送信を防止する。
 * <p/>
 * リダイレクト後のGETリクエストが複数送信された場合には、{@link HttpErrorResponse}を送出する。
 * HTTPステータスコードには、BadRequestであることを示す{@code 400}を設定し、
 * 遷移先のパスには{@link #setForwardPathMapping(Map)}で設定されたパスマッピングを元に設定する。<br/>
 * ※複数送信されたリクエストのリクエストIDが、マッピング設定のキーから始まっている場合その値を遷移先とする。
 * リクエストIDが複数のキーから始まっている場合は、最も長いキーに対応する値を遷移先にする。<br/>
 * 以下にマッピング例を示す。
 * <p/>
 * {@link #setForwardPathMapping(Map)}に設定された内容が以下表の場合、
 * リクエストIDがRW4444の場合は、遷移先のパスは/rw4_error.jspとなる。
 * リクエストIDがRW3333の場合は、遷移先のパスは/rw_error.jspとなる。
 * <pre>
 * ----------- -----------------------------
 * キー        パス
 * ----------- -----------------------------
 * R           /r_error.jsp
 * R1234       /r1234_error.jsp
 * RW          /rw_error.jsp
 * RW4         /rw4_error.jsp
 * ----------- -----------------------------
 * </pre>
 * <p/>
 * ただし、multipartリクエストには未対応。
 * <p/>
 * 本ハンドラは、{@code NablarchTagHandler}の手前に設定すること。
 *
 * @author Kiyohito Itoh
 * @deprecated POST再送信を防止するには、業務アクションにてリダイレクトのレスポンスを返すことで実現してください。
 */
@Deprecated
public class PostResubmitPreventHandler implements HttpRequestHandler {

    /** POST再送信防止を指示するパラメータ */
    public static final String POST_RESUBMIT_PREVENT_PARAM = ExecutionContext.FW_PREFIX + "post_resubmit_prevent";

    /** POST後にリダイレクトされたリクエストを識別するパラメータ */
    private static final String POST_REDIRECT_ID_PARAM = ExecutionContext.FW_PREFIX + "post_redirect_id";

    /** POST時のリクエスト情報をセッションスコープに格納する際に使用するキーのプレフィックス */
    private static final String POST_REQUEST_KEY_PREFIX = ExecutionContext.FW_PREFIX + "post_request_";

    /** リクエストIDと遷移先のパスマッピング */
    private Map<String, String> forwardPathMapping = Collections.emptyMap();

    /**
     * {@inheritDoc}
     * <p/>
     * 処理フローは下記の通り。
     * <pre>
     * [1]POST再送信防止が指示されたリクエストであるか否かを判定する。
     *
     *     POST再送信防止が指示されたリクエストである場合：
     *         リクエスト情報をセッションスコープに格納し、
     *         再度同じURIに対してリダイレクトする。
     *
     *     POST再送信防止が指示されたリクエストでない場合：
     *         [2]に進む。
     *
     * [2]POST後にリダイレクトされたリクエストであるか否かを判定する。
     *
     *     POST後にリダイレクトされたリクエストである場合：
     *         セッションスコープに格納したリクエスト情報をリクエストに設定後、
     *         後続のハンドラを呼び出す。
     *
     *     POST後にリダイレクトされたリクエストでない場合：
     *         後続のハンドラを呼び出す。
     * </pre>
     * 各処理の詳細については、各メソッドのJavadocを参照。
     *
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {

        if (isPostRedirect(request)) {
            context.setSessionScopedVar(
                    generatePostRequestKey(ThreadContext.getExecutionId()),
                    createPostRequest(request));
            return new HttpResponse(generateRedirectPath(request));
        }

        if (isRedirectOnPost(request)) {
            PostRequest postRequest = getPostRequest(request, context);
            if (postRequest == null) {
                throw new HttpErrorResponse(400, getForwardPath(ThreadContext.getRequestId()));
            } else {
                request.setParamMap(postRequest.getParamMap());
            }
        }

        return context.handleNext(request);
    }

    /**
     * 遷移先のパスを取得する。
     *
     * @param requestId リクエストID
     * @return 遷移先のパス
     */
    private String getForwardPath(final String requestId) {
        String path = null;
        int length = 0;
        for (Map.Entry<String, String> entry : forwardPathMapping.entrySet()) {
            if (requestId.startsWith(entry.getKey())) {
                final int keyLength = entry.getKey().length();
                if (length < keyLength) {
                    length = keyLength;
                    path = entry.getValue();
                }
            }
        }
        if (path == null) {
            throw new IllegalArgumentException(
                    "forward path not found in path mapping. request id=[" + requestId + ']');
        }
        return path;
    }

    /**
     * POST再送信防止が指示されたリクエストであるか否かを判定する。
     * <p/>
     * 下記の条件をすべて満たす場合のみtrueを返す。
     * <pre>
     * ・HTTPメソッドがPOSTであること
     * ・POST再送信防止を指示するパラメータ({@link #POST_RESUBMIT_PREVENT_PARAM})が存在すること
     * </pre>
     *
     * @param request リクエスト
     * @return POST再送信防止が指示されたリクエストである場合はtrue
     */
    protected boolean isPostRedirect(HttpRequest request) {
        return "post".equals(request.getMethod().toLowerCase())
                && request.getParamMap().containsKey(POST_RESUBMIT_PREVENT_PARAM);
    }

    /**
     * POST時のリクエスト情報を生成する。
     * <p/>
     * リクエスト情報として下記を取得する。
     * <pre>
     * ・リクエストパラメータ
     * ・マルチパート
     * </pre>
     *
     * @param request リクエスト
     * @return POST時のリクエスト情報
     */
    protected PostRequest createPostRequest(HttpRequest request) {
        return new PostRequest(request);
    }

    /**
     * POST時のリクエスト情報をセッションスコープに格納する際に使用するキーを生成する。
     * キーの形式は下記の通り。
     * <pre>
     *     nablarch_post_request_<実行時ID>
     * </pre>
     * @param executionId 実行時ID
     * @return POST時のリクエスト情報をセッションスコープに格納する際に使用するキー
     */
    protected String generatePostRequestKey(String executionId) {
        return POST_REQUEST_KEY_PREFIX + executionId;
    }

    /**
     * POST後のリダイレクトに使用するパスを生成する。
     * <p/>
     * パスの形式は下記の通り。
     * <pre>
     *     redirect://<リクエストURI>?nablarch_post_redirect_id=<実行時ID>
     * </pre>
     * @param request リクエスト
     * @return POST後のリダイレクトに使用するパス
     */
    protected String generateRedirectPath(HttpRequest request) {
        return new StringBuilder()
            .append("redirect://").append(request.getRequestUri())
            .append("?")
            .append(POST_REDIRECT_ID_PARAM)
            .append("=")
            .append(ThreadContext.getExecutionId())
            .toString();
    }

    /**
     * POST後にリダイレクトされたリクエストであるか否かを判定する。
     * <p/>
     * 下記の条件をすべて満たす場合のみtrueを返す。
     * <pre>
     * ・HTTPメソッドがGETであること
     * ・リダイレクト時に付与したnablarch_post_redirect_idパラメータが存在すること
     * </pre>
     *
     * @param request リクエスト
     * @return POST後にリダイレクトされたリクエストである場合はtrue
     */
    protected boolean isRedirectOnPost(HttpRequest request) {
        return request.getParamMap().containsKey(POST_REDIRECT_ID_PARAM);
    }

    /**
     * リダイレクト前にセッションスコープに格納したリクエスト情報を取得する。
     * <p/>
     * 取得できない場合はnullを返す。<br/>
     * <br/>
     * POST後にリダイレクトされたリクエストを識別するパラメータ、
     * およびPOSTリクエストのリクエスト情報は、
     * ともに保持しているマップから削除する。
     *
     * @param request リクエスト
     * @param context 実行コンテキスト
     * @return リダイレクト前にセッションスコープに格納したリクエスト情報。取得できない場合はnull
     */
    protected PostRequest getPostRequest(HttpRequest request, ExecutionContext context) {
        String[] postRedirectId = request.getParamMap().remove(POST_REDIRECT_ID_PARAM);
        if (StringUtil.isNullOrEmpty(postRedirectId)) {
            return null;
        }
        return (PostRequest) context.getSessionScopeMap()
                .remove(generatePostRequestKey(postRedirectId[0]));
    }

    /**
     * リクエストIDと遷移先パスのマッピングを設定する。
     * <p/>
     * MapのキーにはリクエストIDを識別する値を、値には遷移先のパスを設定する。
     *
     * @param forwardPathMapping マッピング
     */
    public void setForwardPathMapping(final Map<String, String> forwardPathMapping) {
        this.forwardPathMapping = Collections.unmodifiableMap(forwardPathMapping);
    }
}

