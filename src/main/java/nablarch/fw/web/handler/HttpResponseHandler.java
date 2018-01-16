package nablarch.fw.web.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nablarch.common.web.session.SessionUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpResponse.Status;
import nablarch.fw.web.ResourceLocator;
import nablarch.fw.web.ResourceLocatorInternalHelper;
import nablarch.fw.web.ResponseBody;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderFactory;
import nablarch.fw.web.i18n.DirectoryBasedResourcePathRule;
import nablarch.fw.web.i18n.ResourcePathRule;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * ServletAPIを通じてHTTPレスポンス処理を行うハンドラ。
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @author Masato Inoue
 */
public class HttpResponseHandler implements Handler<HttpRequest, HttpResponse> {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(HttpResponseHandler.class);

    /** User-Agentヘッダ。 */
    protected static final String USER_AGENT = "User-Agent";

    /**
     * レスポンスヘッダ設定時にFlushするかどうか
     */
    private boolean usesFlush = true;

    /** HTTPレスポンスコードの変換モード。*/
    private HttpResponseUtil.StatusConvertMode convertMode = HttpResponseUtil.StatusConvertMode.CONVERT_ONLY_400_TO_200;

    /**
     * HTTPヘッダーをwriteした直後にFlushするかどうかの設定
     * デフォルト値はtrueである。
     *
     * @param usesFlush Flushの有無
     *
     */
    public void setForceFlushAfterWritingHeaders(boolean usesFlush) {
        this.usesFlush = usesFlush;
    }

    /** ダウンロードファイル名のエンコーダを取得するクラス。 */
    private DownloadFileNameEncoderFactory
        downloadFileNameEncoderFactory = new DownloadFileNameEncoderFactory();

    /**
     * ダウンロードファイル名のエンコーダを取得するクラスを設定する
     *
     * @param factory ダウンロードファイル名のエンコーダを取得するクラス
     * @return このオブジェクト自体。
     */
    public HttpResponseHandler
    setDownloadFileNameEncoderFactory(DownloadFileNameEncoderFactory factory) {
        downloadFileNameEncoderFactory = factory;
        return this;
    }

    /** ストリームに出力する際のバッファサイズ。 */
    private static final int BUFFER_SIZE = 4096;

    /**
     * HTTPレスポンスコードの変換モードを設定する。<br>
     * <p>
     * HTTPレスポンスコードの変換モードは以下のいずれかである。
     * <ul>
     * <li>CONVERT_ONLY_400_TO_200</li>
     * <li>CONVERT_ALL_TO_200</li>
     * </ul>
     * デフォルトは、CONVERT_ONLY_400_TO_200である。
     * </p>
     * <p>
     * 設定した値は、{@link HttpResponseUtil#chooseResponseStatusCode(HttpResponse, ServletExecutionContext)}で使用される。<br>
     * 変換の仕様については、{@link HttpResponseUtil#chooseResponseStatusCode(HttpResponse, ServletExecutionContext)}を参照。
     * </p>
     * @param convertMode HTTPレスポンスコードの変換モード。
     */
    public void setConvertMode(String convertMode) {
        this.convertMode = HttpResponseUtil.StatusConvertMode.valueOf(convertMode);
    }

    /**
     * {@inheritDoc}
     * <p>
     * この実装では、後続ハンドラの処理結果(HttpResponse)の内容をもとに、
     * クライアントに対するレスポンス処理を行う。
     * </p>
     *
     * @throws ClassCastException
     *   引数 ctx の実際の型が ServletExecutionContext でない場合。
     */
    @Override
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) throws ClassCastException {

        ServletExecutionContext context = (ServletExecutionContext) ctx;

        HttpResponseUtil.setStatusConvertMode(context, convertMode);
        try {
            HttpResponse res = ctx.handleNext(req);
            // サーブレットフォーワード
            if (doesServletForward(res)) {
                try {
                    doServletForward(res, context);
                    return res;
                } catch (ServletException e) {
                    // フォーワード中のエラーはエラーページに遷移させる。
                    // A servlet exception occurred.:
                    if (isClientDisconnected(e)) {
                        LOGGER.logWarn("Uncaught error: ", e);
                    } else {
                        FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
                    }
                    ctx.setException(e.getCause());
                    res = getFatalErrorResponse();
                } catch (IOException e) {
                    // ソケットI/Oでのエラー。 一応ワーニングログだけ出しとく。
                    LOGGER.logWarn("Uncaught error: ", e);
                    return res;
                }
            }

            // リダイレクション
            if (doesRedirect(res)) {
                try {
                    doRedirect(res, context);
                } catch (IOException e) {
                    // ソケットI/Oでのエラー。 一応ワーニングログだけ出しとく。
                    LOGGER.logWarn("Uncaught error: ", e);
                }
                return res;
            }

            // レスポンス処理
            writeResponse(res, context);
            return res;

        } catch (RuntimeException uncaught) {
            try {
                writeResponse(getFatalErrorResponse(), context);
            } catch (Throwable ignored) {
                LOGGER.logDebug("an error occurred during servlet IO.", ignored);
            }
            throw uncaught;

        } catch (Error uncaught) {
            try {
                writeResponse(getFatalErrorResponse(), context);
            } catch (Throwable ignored) {
                LOGGER.logDebug("an error occurred during servlet IO.", ignored);
            }
            throw uncaught;

        } finally {
            ResponseBody.cleanup();
        }
    }

    /**
     * サーブレット例外がクライアントの接続断に起因する例外であるかを判定する。
     * <p>
     * 本実装は、WebLogic 11g のJSP処理中にクライアントの接続断が起きた際に発生する
     * SocketException を原因とする例外の場合、trueを返す。
     * </p>
     * @param e サーブレット例外
     * @return サーブレット例外がクライアントの接続断に起因する例外である場合はtrue
     */
    private boolean isClientDisconnected(ServletException e) {
        Throwable t = e;
        while ((t = t.getCause()) != null) {
            if (t.getClass().isAssignableFrom(SocketException.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * HTTPレスポンスオブジェクトの内容をもとに、
     * クライアントにレスポンスを返す。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキスト
     */
    public void writeResponse(HttpResponse res, ServletExecutionContext ctx) {
        try {
            if (res.isBodyEmpty() && isErrorResponse(res)) {
                ctx.getServletResponse().sendError(res.getStatusCode());
            } else {
                InputStream bodyStream = res.getBodyStream();
                if (bodyStream == null) {
                    // file/classpathスキームのコンテントパスで参照先が存在しない
                    // 場合はシステムエラーとする。
                    bodyStream = getFatalErrorResponse().getBodyStream();
                }
                writeHeaders(res, ctx);
                writeBody(bodyStream, ctx.getServletResponse());
            }
        } catch (IOException e) {
            // ソケットI/Oでのエラー。 一応ワーニングログだけ出しとく。
            if (LOGGER.isWarnEnabled()) {
                LOGGER.logWarn("Uncaught error: ", e);
            }
        } finally {
            res.cleanup();
        }
    }
    
    
    /**
     * レスポンスがエラーか否かを判定する。
     * <p>
     * ステータスコードが400以上であればtrue、
     * それ以外であればfalseを返す。
     * </p>
     * @param res HTTPレスポンス
     * @return 判定結果
     */
    protected boolean isErrorResponse(HttpResponse res) {
        int statusCode = res.getStatusCode();
        return statusCode >= 400;
    }

    /**
     * このリクエストのレスポンス処理においてサーブレットフォーワードを行うか？。
     *
     * @param res HTTPレスポンスオブジェクト
     * @return サーブレットフォーワードを行う場合はtrue。
     */
    private boolean doesServletForward(HttpResponse res) {
        return res.getContentPath() != null
            && res.getContentPath().getScheme().equals("servlet");
    }

    /**
     * このリクエストのレスポンスでリダイレクトを要求する場合かどうかを返す。
     *
     * @param res HTTPレスポンスオブジェクト
     * @return リダイレクトを要求を行う場合はtrue
     */
    private boolean doesRedirect(HttpResponse res) {
        ResourceLocator path = res.getContentPath();
        return path != null
            && path.isRedirect();
    }

    /**
     * サーブレットフォーワード処理を行う。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキスト
     * @throws ServletException フォーワード先においてエラーが発生した場合。
     * @throws IOException レスポンス処理中でのIOエラー
     */
    private void doServletForward(HttpResponse res, ServletExecutionContext ctx)
    throws ServletException, IOException {
        setStatusCode(res, ctx);
        setHeaders(res, ctx);
        String pathToForward = getPathForLanguage(res, ctx);
        if (isSessionExportRequired(pathToForward)) {
            exportSessionStore(ctx);
        }
        ctx.getServletRequest()
           .getRequestDispatcher(pathToForward)
           .forward(ctx.getServletRequest(), ctx.getServletResponse());
    }

    /**
     * セッションストアの内容をリクエストスコープに移送する。
     * 同名のキーがリクエストスコープに存在する場合は、その項目は移送されない。
     *
     * @param ctx {@link ServletExecutionContext}
     */
    private void exportSessionStore(ServletExecutionContext ctx) {
        Map<String, Object> requestScope = ctx.getRequestScopeMap();
        for (String key : ctx.getSessionStoreMap().keySet()) {
            if (!requestScope.containsKey(key)) {
                Object sessionStored = SessionUtil.orNull(ctx, key);
                if (sessionStored != null) {
                    requestScope.put(key, sessionStored);
                }
            }
        }
    }


    /**
     * フォワード先のパスが、セッションを移送すべきパスかどうか判定する。
     * 指定されたpathが"."を含む場合、真と判定する。
     *
     * @param pathToForward フォワード先のパス
     * @return 移送すべき場合、真
     */
    private boolean isSessionExportRequired(String pathToForward) {
        return pathToForward != null && pathToForward.contains(".");
    }

    /**
     * リダイレクト処理を行う。
     * リダイレクト先のURLをURLリライトし、コンテナに処理を委譲する。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキスト
     * @throws IOException リダイレクト処理中のIOエラー
     * @see HttpServletResponse#sendRedirect(String)
     */
    private void doRedirect(HttpResponse res, ServletExecutionContext ctx) throws IOException {
        setHeaders(res, ctx);
        ResourceLocator path = res.getContentPath();
        
        HttpServletResponse servletResponse = ctx.getServletResponse();
        final String to;
        if (ResourceLocatorInternalHelper.isRedirectWithAbsoluteUri(path)) {
            //絶対URIへのリダイレクトの場合はセッションIDが付与されると
            //セキュリティホールになってしまうので
            //HttpServletResponse#encodeRedirectURLは適用しない。
            to = path.getPath();
        } else {
            String rawTo = path.getScheme().matches("https?") ? path.toString()
                      : path.isRelative() ? path.getPath()
                      : ctx.getServletContext().getContextPath() + path.getPath();
            to = servletResponse.encodeRedirectURL(rawTo);
        }

        // 302の場合は、sendRedirectを使用してリダイレクト
        // それ以外の場合は、Locationヘッダを使用してリダイレクト
        if (res.getStatusCode() == Status.FOUND.getStatusCode()) {
            servletResponse.sendRedirect(to);
        } else {
            servletResponse.setStatus(res.getStatusCode());
            servletResponse.setHeader("Location", to);
        }
    }

    /**
     * 言語対応のコンテンツパスを取得する。
     * <p/>
     * 自身の{@link #contentPathRule}プロパティに指定された{@link ResourcePathRule}に処理を委譲する。
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキスト
     * @return 言語対応のコンテンツパス
     */
    private String getPathForLanguage(HttpResponse res, ServletExecutionContext ctx) {
        String path = res.getContentPath().getPath();
        HttpServletRequest req = ctx.getServletRequest();
        return contentPathRule.getPathForLanguage(path, req);
    }

    /** 言語対応コンテンツパスのルール */
    private ResourcePathRule contentPathRule = new DirectoryBasedResourcePathRule();

    /**
     * 言語対応コンテンツパスのルールを設定する。
     * @param contentPathRule 言語対応コンテンツパスのルール
     */
    public void setContentPathRule(ResourcePathRule contentPathRule) {
        this.contentPathRule = contentPathRule;
    }

    /**
     * HTTPステータス・HTTPヘッダの内容をクライアントに送信する。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキストオブジェクト
     * @throws IOException ソケットI/Oにおけるエラー
     */
    private void writeHeaders(HttpResponse            res,
                              ServletExecutionContext ctx)
    throws IOException {
        ctx.getServletResponse().setStatus(res.getStatusCode());
        setHeaders(res, ctx);
        if (usesFlush) {
            ctx.getServletResponse().flushBuffer();
        }
    }

    /**
     * クライアントに送信するステータスコードを設定する。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキスト
     */
    protected static void setStatusCode(HttpResponse res, ServletExecutionContext ctx) {
        HttpServletResponse servletRes = ctx.getServletResponse();
        int statusCode = HttpResponseUtil.chooseResponseStatusCode(res, ctx);
        servletRes.setStatus(statusCode);
    }


    /**
     * サーブレットレスポンスにヘッダを設定する。
     *
     * @param res HTTPレスポンスオブジェクト
     * @param ctx 実行コンテキストオブジェクト
     */
    private void setHeaders(HttpResponse            res,
                            ServletExecutionContext ctx) {

        ctx.getServletResponse().setContentType(res.getContentType());

        for (Map.Entry<String, String> header : res.getHeaderMap().entrySet()) {
            String key = header.getKey();
            String val = header.getValue();
            if ("Content-Length".equals(key)) {
                continue;
            }
            if ("Content-Disposition".equals(key)) {
                val = replaceContentDisposition(val, ctx.getServletRequest());
            }
            ctx.getServletResponse().setHeader(key, val);
        }

        for (Cookie cookie : res.getCookieList()) {
            ctx.getServletResponse().addCookie(cookie);
        }
    }

    /**
     * Content-Dispositionに設定されたファイル名を、エンコーダを用いて変換する。
     *
     * @param dispositionValue Content-Dispositionヘッダの値
     * @param nativeReq サーブレットリクエスト
     * @return ファイル名がエンコードされたContent-Disposition
     */
    private String replaceContentDisposition(String dispositionValue,
                                                    HttpServletRequest nativeReq) {
        ContentDispositionRawValue cdrv = new ContentDispositionRawValue(dispositionValue);
        if (cdrv.needsToBeEncoded() == false) {
            return dispositionValue;
        }
        String userAgent = nativeReq.getHeader(USER_AGENT);
        DownloadFileNameEncoder
        encoder = downloadFileNameEncoderFactory.getEncoder(userAgent);
        String encodedFileName = encoder.encode(cdrv.getRawFileName());
        return cdrv.buildEncodedValue(encodedFileName);
    }

    /**
     * メッセージボディの内容をクライアントに送信する。
     *
     * @param in 入力ストリームの内容
     * @param nativeRes サーブレットレスポンス
     * @throws IOException ソケットI/Oにおけるエラー
     */
    public static void writeBody(InputStream in, HttpServletResponse nativeRes)
    throws IOException {
        OutputStream out = nativeRes.getOutputStream();
        try {
            while (true) {
                byte[] bytes = new byte[BUFFER_SIZE];
                int readBytes = in.read(bytes);
                if (readBytes == -1) {
                    break;
                }
                out.write(bytes, 0, readBytes);
            }
        } finally {
            FileUtil.closeQuietly(in);
            FileUtil.closeQuietly(out);
        }
    }

    /**
     * どうしようも無いときのエラーレスポンスを作成する。
     * @return エラーレスポンス
     */
    private HttpResponse getFatalErrorResponse() {
        return new HttpResponse(500)
              .setContentType("text/html;charset=UTF-8")
              .setBodyStream(new ByteArrayInputStream(
                   fatalErrorMessage.getBytes()
               ));
    }

    /** どうしようも無いときのエラーレスポンスで送信する内容。  */
    private final String fatalErrorMessage = Builder.linesf(
      "<html>"
    , "  <head>"
    , "    <title>A system error occurred.</title>"
    , "  </head>"
    , "  <body>"
    , "    <p>"
    , "      We are sorry not to be able to proceed your request.<br/>"
    , "      Please contact the system administrator of our system."
    , "    </p>"
    , "  </body>"
    , "</html>"
    );
}
