package nablarch.fw.web.upload;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.results.BadRequest;
import nablarch.fw.results.RequestEntityTooLarge;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.HttpRequestWrapper;

/**
 * マルチパートを解析するハンドラ。<br/>
 * <p/>
 * <p>
 * 本ハンドラは、サーブレットの入力ストリーム
 * （{@link jakarta.servlet.http.HttpServletRequest#getInputStream()}）から
 * リクエストのメッセージボディを読み込み、マルチパートの解析を行う。
 * HTTPリクエストがマルチパートでない場合（Content-Typeがmultipart/form-dataでない場合）、
 * 何もせずに後続のハンドラに処理を委譲する。
 * </p>
 * <p>
 * 解析結果は、{@link HttpRequest}に格納される。
 * アップロードファイルに関する情報は{@link HttpRequest#getPart(String)}で取得できる。
 * アップロードファイル以外のパラメータはHTTPリクエストパラメータに格納されるので、通常のように
 * {@link HttpRequest#getParamMap()}等で取得できる。
 * </p>
 * <p>
 * マルチパートの解析前はリクエストパラメータにアクセス出来ないため、
 * リクエストパラメータを扱う以下のハンドラは本ハンドラの後続に配置する必要がある。
 * <ul>
 *     <li>{@link nablarch.common.web.session.SessionStoreHandler}</li>
 *     <li>{@link nablarch.common.web.handler.NablarchTagHandler}</li>
 * </ul>
 * </p>
 * <p>
 * 実際の解析処理は{@link MultipartParser}に委譲され、マルチパートの解析およびアップロードファイルの一時保存が行われる。
 * デフォルト設定では、一時ファイルは本ハンドラの処理終了時点（すなわちHTTPリクエストの処理終了時点）で削除される。
 * </p>
 * <p>
 * アップロードに関する各種設定は{@link UploadSettings}から取得する。
 * </p>
 *
 * @author T.Kawasaki
 */
public class MultipartHandler implements HttpRequestHandler {

    /** 各種設定値 */
    private UploadSettings settings = new UploadSettings();

    /** マルチパート処理状態を格納するキー値 */
    private static final String COMPLETED_FLG_KEY = "nablarch_multipart_completed_flg";

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(MultipartHandler.class);

    /**
     * マルチパート用の各種設定値を設定する。
     *
     * @param settings 各種設定値
     */
    public void setUploadSettings(UploadSettings settings) {
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     *
     * @param request {@link HttpRequestWrapper}でなければならない。
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {

        HttpRequestWrapper wrapper = cast(request);

        if (!MultipartParser.isMultipart(wrapper.getContentType())
                || isParseCompleted(context)) {
            // マルチパートでない＆既にパースが終わっている場合(内部フォーワードで再度呼び出された場合)は、何もしない
            return context.handleNext(request);
        }

        // マルチパートの解析
        final MultipartParser parser = createParser(wrapper, settings);
        final PartInfoHolder parts;
        try {
            parts = parser.parse();
        } catch (BadRequest e) {
            context.setException(e);
            return new HttpResponse(e.getStatusCode());
        } catch (RequestEntityTooLarge e) {
            context.setException(e);
            return new HttpResponse(e.getStatusCode());
        }

        // マルチパートの解析が終わったことをリクエストスコープに設定
        context.setRequestScopedVar(COMPLETED_FLG_KEY, Boolean.TRUE);

        // 解析結果をリクエストに格納する
        request.setMultipart(parts);

        // アップロード情報をログ出力する。
        parts.logAllPart();

        // 以降のハンドラを実行
        try {
            return context.handleNext(request);
        } finally {
            cleanup(parts);
        }
    }

    /**
     * {@link MultipartParser}インスタンスを生成する。
     *
     * @param wrapper  {@link HttpRequestWrapper}
     * @param settings {@link UploadSettings}
     * @return インスタンス
     */
    MultipartParser createParser(HttpRequestWrapper wrapper, UploadSettings settings) {
        return new MultipartParser(
                wrapper.getInputStream(),
                wrapper.getParamMap(),
                settings,
                new MultipartContext(wrapper));
    }

    /**
     * {@link HttpRequest}を{@link HttpRequestWrapper}にキャストする。
     *
     * @param request キャスト対象のHttpRequest
     * @return キャスト後のインスタンス
     * @throws UnsupportedOperationException キャストに失敗した場合。
     */
    private HttpRequestWrapper cast(HttpRequest request)
            throws UnsupportedOperationException {

        try {
            return (HttpRequestWrapper) request;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException(Builder.concat(
                    "MultipartHandler expects ", HttpRequestWrapper.class.getName(),
                    " as HttpRequest. but was ", request.getClass().getName(), "."), e);
        }
    }

    /**
     * マルチパートの解析処理が終了しているか否か。
     * @param context 実行コンテキスト
     * @return 解析が終了している場合は {@code true}
     */
    private boolean isParseCompleted(ExecutionContext context) {
        Boolean competedFlg = context.getRequestScopedVar(COMPLETED_FLG_KEY);
        return competedFlg != null && competedFlg;
    }

    /**
     * 一時保存されたアップロードファイルを削除する。
     *
     * @param parts パート情報
     */
    private void cleanup(PartInfoHolder parts) {
        if (settings.isAutoCleaning()) {
            try {
                parts.cleanup();
            } catch (Throwable t) {
                LOGGER.logWarn("failed to delete temp file.", t);
            }
        }
    }
}
