package nablarch.fw.web.handler;

import static nablarch.fw.ExecutionContext.FW_PREFIX;

import java.util.Map;

import nablarch.core.log.LogUtil;
import nablarch.core.log.LogUtil.ObjectCreator;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.util.ObjectUtil;
import nablarch.fw.Request;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.handler.HttpAccessLogFormatter.HttpAccessLogContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTPアクセスログを出力するクラス。<br>
 * ロガー名は"HTTP_ACCESS"を使用し、INFOレベルで出力する。
 * @author Kiyohito Itoh
 */
public final class HttpAccessLogUtil {
    
    /** 隠蔽コンストラクタ */
    private HttpAccessLogUtil() {
    }
    
    /** HTTPアクセスログを出力するロガー */
    private static final Logger HTTP_ACCESS_LOGGER = LoggerManager.get("HTTP_ACCESS");
    
    /** {@link HttpAccessLogFormatter}のクラス名 */
    private static final String PROPS_CLASS_NAME = HttpAccessLogFormatter.PROPS_PREFIX + "className";
    
    /** {@link HttpAccessLogFormatter}を生成する{@link ObjectCreator} */
    private static final ObjectCreator<HttpAccessLogFormatter> HTTP_ACCESS_LOG_FORMATTER_CREATOR = new ObjectCreator<HttpAccessLogFormatter>() {
        public HttpAccessLogFormatter create() {
            HttpAccessLogFormatter formatter = null;
            Map<String, String> props = AppLogUtil.getProps();
            if (props.containsKey(PROPS_CLASS_NAME)) {
                String className =  props.get(PROPS_CLASS_NAME);
                formatter = ObjectUtil.createInstance(className);
            } else {
                formatter = new HttpAccessLogFormatter();
            }
            return formatter;
        }
    };
    
    /**
     * クラスローダに紐付く{@link HttpAccessLogFormatter}を生成する。
     */
    public static void initialize() {
        getAccessLogFormatter();
    }
    
    /** {@link HttpAccessLogContext}をリクエストスコープに格納する際に使用するキー */
    private static final String KEY_HTTP_ACCESS_LOG_CONTEXT = FW_PREFIX + "httpAccessLogContext";
    
    /**
     * リクエストスコープから{@link HttpAccessLogContext}を取得する。<br>
     * 存在しない場合は{@link HttpAccessLogContext}を生成し、リクエストスコープに設定する。
     * @param context {@link ServletExecutionContext}
     * @param request {@link HttpRequest}
     * @return {@link HttpAccessLogContext}
     */
    public static HttpAccessLogContext getAccessLogContext(Request<?> request, ServletExecutionContext context) {
        Map<String, Object> requestScope = context.getRequestScopeMap();
        if (requestScope.containsKey(KEY_HTTP_ACCESS_LOG_CONTEXT)) {
            return (HttpAccessLogContext) requestScope.get(KEY_HTTP_ACCESS_LOG_CONTEXT);
        }

        HttpAccessLogContext logContext = getAccessLogFormatter().createAccessLogContext();
        requestScope.put(KEY_HTTP_ACCESS_LOG_CONTEXT, logContext);
        logContext.setContext(context);
        logContext.setRequest((HttpRequest) request);
        
        return logContext;
    }
    
    /**
     * クラスローダに紐付く{@link HttpAccessLogFormatter}を取得する。
     * @return {@link HttpAccessLogFormatter}
     */
    private static HttpAccessLogFormatter getAccessLogFormatter() {
        return LogUtil.getObjectBoundToClassLoader(HTTP_ACCESS_LOG_FORMATTER_CREATOR);
    }
    
    /**
     * 出力対象にメモリ項目が含まれているか否かを判定する。
     * @return 出力対象にメモリ項目が含まれている場合はtrue
     */
    public static boolean containsMemoryItem() {
        return getAccessLogFormatter().containsMemoryItem();
    }
    
    /**
     * リクエスト処理開始時のログを出力する。
     * @param context {@link HttpAccessLogContext}
     * @param logOptions ログ出力のオプション情報
     */
    public static void begin(HttpAccessLogContext context, Object[] logOptions) {
        HttpAccessLogFormatter formatter = getAccessLogFormatter();
        if (formatter.isBeginOutputEnabled()) {
            HTTP_ACCESS_LOGGER.logInfo(getAccessLogFormatter().formatBegin(context), logOptions);
        }
    }
    
    /**
     * hiddenパラメータ復号後のログを出力する。
     * @param context {@link HttpAccessLogContext}
     */
    public static void logParameters(HttpAccessLogContext context) {
        HttpAccessLogFormatter formatter = getAccessLogFormatter();
        if (formatter.isParametersOutputEnabled()) {
            HTTP_ACCESS_LOGGER.logInfo(formatter.formatParameters(context));
        }
    }
    
    /**
     * ディスパッチ先クラス決定後のログを出力する。
     * @param context {@link HttpAccessLogContext}
     */
    public static void logDispatchingClass(HttpAccessLogContext context) {
        HttpAccessLogFormatter formatter = getAccessLogFormatter();
        if (formatter.isDispatchingClassOutputEnabled()) {
            HTTP_ACCESS_LOGGER.logInfo(getAccessLogFormatter().formatDispatchingClass(context));
        }
    }
    
    /**
     * リクエスト処理終了時のログを出力する。
     * @param context {@link HttpAccessLogContext}
     * @param logOptions ログ出力のオプション情報
     */
    public static void end(HttpAccessLogContext context, Object[] logOptions) {
        HttpAccessLogFormatter formatter = getAccessLogFormatter();
        if (formatter.isEndOutputEnabled()) {
            HTTP_ACCESS_LOGGER.logInfo(getAccessLogFormatter().formatEnd(context), logOptions);
        }
    }
}
