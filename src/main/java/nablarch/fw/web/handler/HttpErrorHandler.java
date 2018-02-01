package nablarch.fw.web.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.message.ApplicationException;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.NoMoreHandlerException;
import nablarch.fw.Result;
import nablarch.fw.results.ServiceError;
import nablarch.fw.web.message.ErrorMessages;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.ResourceLocator;

/**
 * 共通エラーハンドラー。
 * <pre>
 * HttpResponse/HttpErrorResponse のHTTPエラーコードに対応した
 * エラー画面に遷移させる。
 * また、実行時例外を捕捉し、システムエラー画面に遷移させる。
 * </pre>
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class HttpErrorHandler implements HttpRequestHandler {
    /** ロガー */
    private static final Logger
        LOGGER = LoggerManager.get(HttpErrorHandler.class);
    
    /** {@link Result.Error}の中で障害通知ログを出力する対象を表すステータスコード */
    protected Pattern writeFailureLogPattern = Pattern.compile("5([1-9][0-9]|0[012456789])"); // SUPPRESS CHECKSTYLE サブクラスで使用するフィールドのため。

    /** {@inheritDoc} */
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        HttpResponse res = null;
        try {
            res = ctx.handleNext(req);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.logTrace("HTTP Response: ", res, res.getContentPath());
            }
            
        } catch (NoMoreHandlerException e) {
            // ハンドラキューが空になり、後続ハンドラに処理を
            // 委譲できなかった場合は、404エラーをレスポンスする。
            LOGGER.logInfo(Builder.concat("There were no Handlers in handlerQueue.",
                                          " uri = [", req.getRequestUri(), "]"));
            res = HttpResponse.Status.NOT_FOUND.handle(req, ctx);
            
        } catch (HttpErrorResponse e) {
            // HTTPエラーコードに対応したアプリケーションエラー。
            // レスポンスオブジェクトに従った画面遷移を行う。
            ctx.setException(e.getCause());
            res = e.getResponse();
            
            // 原因例外がApplicationExceptionの場合はテンプレートエンジンで扱えるよう、
            // メッセージを保持するオブジェクトに変換しリクエストスコープに格納する。
            if (e.getCause() instanceof ApplicationException) {
                final WebConfig webConfig = WebConfigFinder.getWebConfig();
                ctx.setRequestScopedVar(webConfig.getErrorMessageRequestAttributeName(),
                        new ErrorMessages((ApplicationException) e.getCause()));
            }
            
        } catch (Result.Error e) {
            // 共通ハンドラ等から送出される汎用例外。
            // 対応するHTTPステータスコードのエラー画面をレスポンスする。
            if (writeFailureLogPattern.matcher(String.valueOf(e.getStatusCode())).matches()) {
                if (e instanceof nablarch.fw.results.InternalError) {
                    ((ServiceError) e).writeLog(ctx);
                } else {
                    FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
                }
            }
            ctx.setException(e.getCause());
            res = new HttpResponse(e.getStatusCode());

        } catch (RuntimeException e) {
            // 未捕捉の実行時例外が発生した場合はエラーログを出力し、
            // HTTPステータス500のレスポンスオブジェクトを返却する。
            // Uncaught runtime exception:
            FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
            ctx.setException(e);
            res = HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);
            
        } catch (StackOverflowError e) {
            // 無限ループのバグの可能性が高いので、通常のエラー扱い。
            // Uncaught Error:
            FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
            ctx.setException(e);
            res = HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);
            
        } catch (ThreadDeath e) {
            throw e;
        } catch (VirtualMachineError e) {    
            throw e;
            // アプリケーション側で対処すべき状況ではないので、
            // このハンドラでは特段の処理を行わず上位にリスローする。

            
        } catch (Error e) {
            // 上記以外のエラーについてはログ出力後、
            // ステータスコード500のレスポンスを返す。
            FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
            ctx.setException(e);
            res = HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);
        }
        
        if (res.isBodyEmpty()) {
            res.setContentPath(getDefaultPageFor(res.getStatusCode()));
        }
        return res;
    }

    /**
     * レスポンスステータスコードごとのデフォルトページを設定する。
     * 
     * HttpResponseオブジェクトのボディ内容(contentPath/contentBody)が設定されていない場合、
     * ここでステータスコード毎に設定したデフォルトページがボディとしてレスポンスされる。
     * 設定は後から設定した内容ほど優先される。
     * ステータスコードには1桁分のワイルドカードとして "." を使用することができる。
     * 
     * 設定例::
     * <pre>
     *   // デフォルトページ定義
     *   setDefaultPage("303", "file:///www/docroot/redirecting.html");
     *   setDefaultPage("4..", "servlet://jsp/errors/userError.jsp");
     *   setDefaultPage("5..", "servlet://jsp/errors/systemError.jsp");
     * </pre>
     * 
     * デフォルトページの設定を行わない場合、
     * web.xmlに定義されているエラーページに遷移する。
     *
     * @param statusCode ステータスコードのパターン
     * @param contentPath デフォルトページのコンテンツパス
     * @return このオブジェクト自体
     */
    public HttpErrorHandler
    setDefaultPage(String statusCode, String contentPath) {
        if (!statusCode.matches("[0-9.]{3}")) {
            throw new RuntimeException("invalid status code format.: " + contentPath);
        }
        defaultPages.put(statusCode, ResourceLocator.valueOf(contentPath));
        return this;
    }
    
    /**
     * レスポンスステータスコードごとのデフォルトページを設定する。
     * @param defaultPages デフォルトページ設定
     * @return このオブジェクト自体
     */
    public HttpErrorHandler
    setDefaultPages(Map<String, String> defaultPages) {
        this.defaultPages.clear();
        if (defaultPages != null) {
            for (Entry<String, String> defaultPage : defaultPages.entrySet()) {
                setDefaultPage(defaultPage.getKey(), defaultPage.getValue());
            }
        }
        return this;
    }

    /**
     * 指定されたステータスコードに対するデフォルトページのコンテンツパスを返す。
     * @param statusCode ステータスコード
     * @return デフォルト画面のコンテンツパス
     */
    public ResourceLocator getDefaultPageFor(int statusCode) {
        String statusCodeStr = Integer.valueOf(statusCode).toString();
        int hiScore = 0;
        ResourceLocator result = null;
        if (defaultPages != null) {
            for (Map.Entry<String, ResourceLocator> entry : defaultPages.entrySet())  {
                if (entry.getKey().indexOf('.') == -1) {
                    if (statusCodeStr.equals(entry.getKey())) {
                        return entry.getValue();
                    }
                } else {
                    int score = matchingScore(statusCodeStr, entry.getKey());
                    if (hiScore < score) {
                        result = entry.getValue();
                        hiScore = score;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * 一致文字数を計数する。
     * @param aStr 基準文字列
     * @param bStr 比較対象文字列
     * @return 一致する文字数
     */
    private int matchingScore(String aStr, String bStr) {
        int score = 0;
        for (int i = 0; i < aStr.length(); i++) {
            if (i >= bStr.length()) {
                break;
            }
            if (aStr.charAt(i) == bStr.charAt(i)) {
                score++;
            }
        }
        return score;
    }

    /** デフォルトページのドキュメントルート */
    private static final String DOCROOT = "servlet:///jsp/";
    
    /** 
     * 既定のデフォルトページ
     */
    private final Map<String, ResourceLocator> defaultPages = new HashMap<String, ResourceLocator>();

    /**
     * {@link #handle(nablarch.fw.web.HttpRequest, nablarch.fw.ExecutionContext)}で、{@link Result.Error}を補足した際に、
     * 障害通知ログを出力する必要のあるステータスコードを正規表現で設定する。
     * 
     * ここで設定した正規表現が、{@link nablarch.fw.Result.Error#getStatusCode()}にマッチした場合のみ、
     * 障害通知ログが出力され障害として検知される。
     * なお、本設定を省略した場合のデフォルト動作では、{@code 5([1-9][0-9]|0[012456789])}に一致するステータスコードが障害通知ログの出力対象となる。
     * @param writeFailureLogPattern 障害通知対象のステータスコードを表す正規表現
     */
    public void setWriteFailureLogPattern(String writeFailureLogPattern) {
        this.writeFailureLogPattern = Pattern.compile(writeFailureLogPattern);
    }
}
