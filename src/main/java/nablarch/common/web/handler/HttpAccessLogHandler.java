package nablarch.common.web.handler;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpAccessLogFormatter.HttpAccessLogContext;
import nablarch.fw.web.handler.HttpAccessLogUtil;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTPアクセスログを出力するクラス。
 * <pre>
 * ロガー名は"HTTP_ACCESS"を使用し、INFOレベルで出力する。
 * {@link #handle(HttpRequest, ExecutionContext)}メソッドの引数{@link ExecutionContext}は、
 * リクエスト情報を取得するために{@link nablarch.fw.web.servlet.ServletExecutionContext}にダウンキャストして使用する。
 * </pre>
 * @author Kiyohito Itoh
 */
public class HttpAccessLogHandler implements Handler<HttpRequest, HttpResponse> {
    
    /**
     * {@link nablarch.fw.web.handler.HttpAccessLogFormatter}を初期化する。
     */
    public HttpAccessLogHandler() {
        HttpAccessLogUtil.initialize();
    }
    /**
     * HTTPアクセスログを出力する。
     * @param req  {@link HttpRequest}
     * @param context {@link ExecutionContext}
     * @return 次のハンドラの処理結果
     * @throws ClassCastException
     *   context の型が {@link ServletExecutionContext} でない場合。
     */
    public HttpResponse handle(HttpRequest req, ExecutionContext context)
    throws ClassCastException {
        ServletExecutionContext ctx = (ServletExecutionContext) context;
        HttpAccessLogContext logContext = HttpAccessLogUtil.getAccessLogContext(req, ctx);
        writeBeginLog(req, ctx, logContext);
        
        HttpResponse response = null;
        try {
            response = context.handleNext(req);
        } catch (HttpErrorResponse errorResponse) {
            response = errorResponse.getResponse();
            throw errorResponse;
        } finally {
            writeEndLog(req, ctx, logContext, response);
        }
        
        return response;
    }
    
    /**
     * リクエスト処理開始時のログを出力する。
     * @param request {@link HttpRequest}
     * @param context {@link ExecutionContext}
     * @param logContext {@link HttpAccessLogContext}
     */
    protected void writeBeginLog(HttpRequest request, ServletExecutionContext context, HttpAccessLogContext logContext) {
        
        Object[] requestLogOptions = getRequestOptions(request, context);
        HttpAccessLogUtil.begin(logContext, requestLogOptions);
        
        if (HttpAccessLogUtil.containsMemoryItem()) {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemory = memory.getHeapMemoryUsage();
            long max = heapMemory.getMax();
            logContext.setMaxMemory(max);
            logContext.setFreeMemory(max - heapMemory.getUsed());
        }
        
        logContext.setStartTime(System.currentTimeMillis());
    }
    
    /**
     * リクエスト処理終了時のログを出力する。
     * @param request {@link HttpRequest}
     * @param context {@link ExecutionContext}
     * @param logContext {@link HttpAccessLogContext}
     * @param response {@link HttpResponse}
     */
    protected void writeEndLog(HttpRequest request, ServletExecutionContext context, HttpAccessLogContext logContext, HttpResponse response) {
        
        logContext.setEndTime(System.currentTimeMillis());
        
        if (response != null) {
            logContext.setResponse(response);
        }
        
        Object[] responseOptions = getResponseOptions(request, response, context);
        HttpAccessLogUtil.end(logContext, responseOptions);
    }
    
    /** 空のオプション情報 */
    private static final Object[] EMPTY_OPTIONS = new Object[0];
    
    /**
     * リクエスト時のオプション情報を取得する。<br>
     * デフォルト実装ではnullを返す。
     * @param request {@link HttpRequest}
     * @param context {@link ExecutionContext}
     * @return オプション情報。指定しない場合はnull
     */
    protected Object[] getRequestOptions(HttpRequest request, ExecutionContext context) {
        return EMPTY_OPTIONS;
    }
    
    /**
     * レスポンス時のオプション情報を取得する。<br>
     * デフォルト実装ではnullを返す。
     * @param request {@link HttpRequest}
     * @param response {@link HttpResponse}
     * @param context {@link ExecutionContext}
     * @return オプション情報。指定しない場合はnull
     */
    protected Object[] getResponseOptions(HttpRequest request, HttpResponse response, ExecutionContext context) {
        return EMPTY_OPTIONS;
    }
}
