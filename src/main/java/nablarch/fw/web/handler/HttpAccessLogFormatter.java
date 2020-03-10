package nablarch.fw.web.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nablarch.core.ThreadContext;
import nablarch.core.log.DateItemSupport;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.MaskingMapItemSupport;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.ResourceLocator;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTPアクセスログのメッセージをフォーマットするクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class HttpAccessLogFormatter {

    /** デフォルトの日時フォーマット */
    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /** デフォルトのリクエスト処理開始時のフォーマット */
    private static final String DEFAULT_BEGIN_FORMAT = "@@@@ BEGIN @@@@ rid = [$requestId$] uid = [$userId$] sid = [$sessionId$]"
                                                        + "\n\turl         = [$url$]"
                                                        + "\n\tmethod      = [$method$]"
                                                        + "\n\tport        = [$port$]"
                                                        + "\n\tclient_ip   = [$clientIpAddress$]"
                                                        + "\n\tclient_host = [$clientHost$]";

    /** デフォルトのhiddenパラメータ復号後のフォーマット */
    private static final String DEFAULT_PARAMETERS_FORMAT = "@@@@ PARAMETERS @@@@"
                                                            + "\n\tparameters  = [$parameters$]";

     /** デフォルトのディスパッチ先クラス決定後のフォーマット */
    private static final String DEFAULT_DISPATCHING_CLASS_FORMAT = "@@@@ DISPATCHING CLASS @@@@ class = [$dispatchingClass$]";

    /** デフォルトのリクエスト処理終了時のフォーマット */
    private static final String DEFAULT_END_FORMAT = "@@@@ END @@@@ rid = [$requestId$] uid = [$userId$] sid = [$sessionId$]"
                                                      + " url = [$url$] status_code = [$statusCode$] content_path = [$contentPath$]"
                                                      + "\n\tstart_time     = [$startTime$]"
                                                      + "\n\tend_time       = [$endTime$]"
                                                      + "\n\texecution_time = [$executionTime$]"
                                                      + "\n\tmax_memory     = [$maxMemory$]"
                                                      + "\n\tfree_memory    = [$freeMemory$]";

    /** デフォルトのマスク文字 */
    private static final String DEFAULT_MASKING_CHAR = "*";

    /** デフォルトのマスク対象のパターン */
    private static final Pattern[] DEFAULT_MASKING_PATTERNS = new Pattern[0];

    /** デフォルトのリクエストパラメータの区切り文字 */
    private static final String DEFAULT_PARAMETERS_SEPARATOR = Logger.LS + "\t\t";

    /** デフォルトのセッションスコープ情報の区切り文字 */
    private static final String DEFAULT_SESSION_SCOPE_SEPARATOR = Logger.LS + "\t\t";

    /** デフォルトのリクエスト処理開始時の出力が有効か否か。 */
    private static final String DEFAULT_BEGIN_OUTPUT_ENABLED = Boolean.TRUE.toString();

    /** デフォルトのhiddenパラメータ復号後の出力が有効か否か。 */
    private static final String DEFAULT_PARAMETERS_OUTPUT_ENABLED = Boolean.TRUE.toString();

    /** デフォルトのディスパッチ先クラス決定後の出力が有効か否か。 */
    private static final String DEFAULT_DISPATCHING_CLASS_OUTPUT_ENABLED = Boolean.TRUE.toString();

    /** デフォルトのリクエスト処理終了時の出力が有効か否か。 */
    private static final String DEFAULT_END_OUTPUT_ENABLED = Boolean.TRUE.toString();

    /** プロパティ名のプレフィックス */
    public static final String PROPS_PREFIX = "httpAccessLogFormatter.";

    /** リクエスト処理開始時のフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_BEGIN_FORMAT = PROPS_PREFIX + "beginFormat";

    /** hiddenパラメータ復号後のフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_PARAMETERS_FORMAT = PROPS_PREFIX + "parametersFormat";

    /** ディスパッチ先クラス決定後のフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_DISPATCHING_CLASS_FORMAT = PROPS_PREFIX + "dispatchingClassFormat";

    /** リクエスト処理終了時のフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_END_FORMAT = PROPS_PREFIX + "endFormat";

    /** 開始日時と終了日時のフォーマットに使用する日時パターンを取得する際に使用するプロパティ名 */
    private static final String PROPS_DATE_PATTERN = PROPS_PREFIX + "datePattern";

    /** マスク文字を取得する際に使用するプロパティ名 */
    private static final String PROPS_MASKING_CHAR = PROPS_PREFIX + "maskingChar";

    /** マスク対象のパターンを取得する際に使用するプロパティ名 */
    private static final String PROPS_MASKING_PATTERNS = PROPS_PREFIX + "maskingPatterns";

    /** リクエストパラメータの区切り文字を取得する際に使用するプロパティ名 */
    private static final String PROPS_PARAMETERS_SEPARATOR = PROPS_PREFIX + "parametersSeparator";

    /** セッションスコープ情報の区切り文字を取得する際に使用するプロパティ名 */
    private static final String PROPS_SESSION_SCOPE_SEPARATOR = PROPS_PREFIX + "sessionScopeSeparator";

    /** リクエスト処理開始時の出力が有効か否かを取得する際に使用するプロパティ名 */
    private static final String PROPS_BEGIN_OUTPUT_ENABLED = PROPS_PREFIX + "beginOutputEnabled";

    /** hiddenパラメータ復号後の出力が有効か否かを取得する際に使用するプロパティ名 */
    private static final String PROPS_PARAMETERS_OUTPUT_ENABLED = PROPS_PREFIX + "parametersOutputEnabled";

    /** ディスパッチ先クラス決定後の出力が有効か否かを取得する際に使用するプロパティ名 */
    private static final String PROPS_DISPATCHING_CLASS_OUTPUT_ENABLED = PROPS_PREFIX + "dispatchingClassOutputEnabled";

    /** リクエスト処理終了時の出力が有効か否かを取得する際に使用するプロパティ名 */
    private static final String PROPS_END_OUTPUT_ENABLED = PROPS_PREFIX + "endOutputEnabled";

    /** リクエスト処理開始時の出力が有効か否か。 */
    private boolean beginOutputEnabled;

    /** hiddenパラメータ復号後の出力が有効か否か。 */
    private boolean parametersOutputEnabled;

    /** ディスパッチ先クラス決定後の出力が有効か否か。 */
    private boolean dispatchingClassOutputEnabled;

    /** リクエスト処理終了時の出力が有効か否か。 */
    private boolean endOutputEnabled;

    /** 出力対象にメモリ項目が含まれているか否か。 */
    private boolean containsMemoryItem = false;

    /** リクエスト処理開始時のフォーマット済みのログ出力項目 */
    private LogItem<HttpAccessLogContext>[] beginLogItems;

    /** hiddenパラメータ復号後のフォーマット済みのログ出力項目 */
    private LogItem<HttpAccessLogContext>[] parametersLogItems;

    /** ディスパッチ先クラス決定後のフォーマット済みのログ出力項目 */
    private LogItem<HttpAccessLogContext>[] dispatchingClassLogItems;

    /** リクエスト処理終了時のフォーマット済みのログ出力項目 */
    private LogItem<HttpAccessLogContext>[] endLogItems;

    /**
     * フォーマット済みのログ出力項目を初期化する。
     */
    public HttpAccessLogFormatter() {

        Map<String, String> props = AppLogUtil.getProps();
        Map<String, LogItem<HttpAccessLogContext>> logItems = getLogItems(props);

        beginOutputEnabled = Boolean.valueOf(getProp(props, PROPS_BEGIN_OUTPUT_ENABLED, DEFAULT_BEGIN_OUTPUT_ENABLED));
        if (beginOutputEnabled) {
            beginLogItems = LogUtil.createFormattedLogItems(logItems, getProp(props, PROPS_BEGIN_FORMAT, DEFAULT_BEGIN_FORMAT));
        }

        parametersOutputEnabled = Boolean.valueOf(getProp(props, PROPS_PARAMETERS_OUTPUT_ENABLED, DEFAULT_PARAMETERS_OUTPUT_ENABLED));
        if (parametersOutputEnabled) {
            parametersLogItems = LogUtil.createFormattedLogItems(logItems, getProp(props, PROPS_PARAMETERS_FORMAT, DEFAULT_PARAMETERS_FORMAT));
        }

        dispatchingClassOutputEnabled = Boolean.valueOf(getProp(props, PROPS_DISPATCHING_CLASS_OUTPUT_ENABLED, DEFAULT_DISPATCHING_CLASS_OUTPUT_ENABLED));
        if (dispatchingClassOutputEnabled) {
            dispatchingClassLogItems = LogUtil.createFormattedLogItems(
                    logItems, getProp(props, PROPS_DISPATCHING_CLASS_FORMAT, DEFAULT_DISPATCHING_CLASS_FORMAT));
        }

        endOutputEnabled = Boolean.valueOf(getProp(props, PROPS_END_OUTPUT_ENABLED, DEFAULT_END_OUTPUT_ENABLED));
        if (endOutputEnabled) {
            endLogItems = LogUtil.createFormattedLogItems(logItems, getProp(props, PROPS_END_FORMAT, DEFAULT_END_FORMAT));
            containsMemoryItem = LogUtil.contains(endLogItems, MaxMemoryItem.class, FreeMemoryItem.class);
        }
    }

    /**
     * HttpAccessLogContextを生成する。
     * @return HttpAccessLogContext
     */
    public HttpAccessLogContext createAccessLogContext() {
        return new HttpAccessLogContext();
    }

    /**
     * 出力対象にメモリ項目が含まれているか否かを判定する。
     * @return 出力対象にメモリ項目が含まれている場合はtrue
     */
    public boolean containsMemoryItem() {
        return containsMemoryItem;
    }

    /**
     * フォーマット対象のログ出力項目を取得する。
     * @param props 各種ログの設定情報
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, LogItem<HttpAccessLogContext>> getLogItems(Map<String, String> props) {
        Map<String, LogItem<HttpAccessLogContext>> logItems = new HashMap<String, LogItem<HttpAccessLogContext>>();
        logItems.put("$requestId$", new RequestIdItem());
        logItems.put("$userId$", new UserIdItem());
        logItems.put("$url$", new UrlItem());
        logItems.put("$rawUrl$", new RawURLItem());
        logItems.put("$port$", new PortItem());
        logItems.put("$method$", new MethodItem());
        char maskingChar = getMaskingChar(props);
        Pattern[] maskingPatterns = getMaskingPatterns(props);
        logItems.put("$parameters$", new ParametersItem(maskingChar, maskingPatterns,
                                                        getSeparator(props, PROPS_PARAMETERS_SEPARATOR, DEFAULT_PARAMETERS_SEPARATOR)));
        logItems.put("$sessionScope$", new SessionScopeItem(maskingChar, maskingPatterns,
                                                            getSeparator(props, PROPS_SESSION_SCOPE_SEPARATOR, DEFAULT_SESSION_SCOPE_SEPARATOR)));
        logItems.put("$dispatchingClass$", new DispatchingClassItem());
        logItems.put("$sessionId$", new SessionIdItem());
        logItems.put("$statusCode$", new StatusCodeItem());
        logItems.put("$responseStatusCode$", new ResponseStatusCodeItem());

        logItems.put("$contentPath$", new ContentPathItem());
        logItems.put("$clientIpAddress$", new ClientIpAddressItem());
        logItems.put("$clientHost$", new ClientHostItem());
        logItems.put("$clientUserAgent$", new ClientUserAgentItem());
        DateFormat dateFormat = getDateFormat(props);
        logItems.put("$startTime$", new StartTimeItem(dateFormat));
        logItems.put("$endTime$", new EndTimeItem(dateFormat));
        logItems.put("$executionTime$", new ExecutionTimeItem());
        logItems.put("$maxMemory$", new MaxMemoryItem());
        logItems.put("$freeMemory$", new FreeMemoryItem());

        return logItems;
    }

    /**
     * 日時フォーマットを取得する。<br>
     * プロパティの指定がない場合はデフォルトの日時フォーマットを返す。
     * @param props 各種ログの設定情報
     * @return 日時フォーマット
     */
    protected DateFormat getDateFormat(Map<String, String> props) {
        String datePattern = props.get(PROPS_DATE_PATTERN);
        return datePattern != null ? new SimpleDateFormat(datePattern) : DEFAULT_DATE_FORMAT;
    }

    /**
     * プロパティを取得する。<br>
     * プロパティの指定がない場合はデフォルト値を返す。
     * @param props 各種ログの設定情報
     * @param propName プロパティ名
     * @param defaultValue プロパティのデフォルト値
     * @return プロパティ
     */
    protected String getProp(Map<String, String> props, String propName, String defaultValue) {
        String value = props.get(propName);
        return value != null ? value : defaultValue;
    }

    /**
     * 区切り文字を取得する。
     * @param props 各種ログの設定情報
     * @param propName プロパティ名
     * @param defaultValue プロパティのデフォルト値
     * @return パラメータ間の区切り文字
     */
    protected String getSeparator(Map<String, String> props, String propName, String defaultValue) {
        String parametersSeparator = getProp(props, propName, defaultValue);
        return parametersSeparator.replace("\\n", Logger.LS).replace("\\t", "\t");
    }

    /**
     * マスク文字を取得する。
     * @param props 各種ログの設定情報
     * @return マスク文字
     */
    protected char getMaskingChar(Map<String, String> props) {
        String maskingChar = getProp(props, PROPS_MASKING_CHAR, DEFAULT_MASKING_CHAR);
        if (maskingChar.toCharArray().length != 1) {
            throw new IllegalArgumentException(
                String.format("maskingChar was not char type. maskingChar = [%s]", maskingChar));
        }
        return maskingChar.charAt(0);
    }

    /**
     * マスク対象のパラメータ名を取得する。<br>
     * プロパティの指定がない場合はデフォルト値を返す。
     * @param props 各種ログの設定情報
     * @return マスク対象のパラメータ名
     */
    protected Pattern[] getMaskingPatterns(Map<String, String> props) {
        String patterns = props.get(PROPS_MASKING_PATTERNS);
        if (patterns == null) {
            return DEFAULT_MASKING_PATTERNS;
        }
        String[] splitPatterns = MULTIVALUE_SEPARATOR_PATTERN.split(patterns);
        List<Pattern> maskingPatterns = new ArrayList<Pattern>();
        for (String regex : splitPatterns) {
            regex = regex.trim();
            if (StringUtil.isNullOrEmpty(regex)) {
                continue;
            }
            maskingPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
        return maskingPatterns.toArray(new Pattern[maskingPatterns.size()]);
    }

    /** 多値指定(カンマ区切り)のプロパティを分割する際に使用するパターン */
    private static final Pattern MULTIVALUE_SEPARATOR_PATTERN = Pattern.compile(",");

    /**
     * リクエスト処理開始時のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    public String formatBegin(HttpAccessLogContext context) {
        return LogUtil.formatMessage(beginLogItems, context);
    }

    /**
     * hiddenパラメータ復号後のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    public String formatParameters(HttpAccessLogContext context) {
        return LogUtil.formatMessage(parametersLogItems, context);
    }

    /**
     * ディスパッチ先クラス決定後のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    public String formatDispatchingClass(HttpAccessLogContext context) {
        return LogUtil.formatMessage(dispatchingClassLogItems, context);
    }

    /**
     * リクエスト処理終了時のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    public String formatEnd(HttpAccessLogContext context) {
        return LogUtil.formatMessage(endLogItems, context);
    }

    /**
     * リクエスト処理開始時の出力が有効かを判定する。
     * @return リクエスト処理開始時の出力が有効な場合はtrue。
     */
    public boolean isBeginOutputEnabled() {
        return beginOutputEnabled;
    }

    /**
     * hiddenパラメータ復号後の出力が有効かを判定する。
     * @return hiddenパラメータ復号後の出力が有効な場合はtrue。
     */
    public boolean isParametersOutputEnabled() {
        return parametersOutputEnabled;
    }

    /**
     * ディスパッチ先クラス決定後の出力が有効かを判定する。
     * @return ディスパッチ先クラス決定後の出力が有効な場合はtrue。
     */
    public boolean isDispatchingClassOutputEnabled() {
        return dispatchingClassOutputEnabled;
    }

    /**
     * リクエスト処理終了時の出力が有効かを判定する。
     * @return リクエスト処理終了時の出力が有効な場合はtrue。
     */
    public boolean isEndOutputEnabled() {
        return endOutputEnabled;
    }

    /**
     * HTTPアクセスログの出力項目を保持するクラス。
     * @author Kiyohito Itoh
     */
    public static class HttpAccessLogContext {
        /** {@link nablarch.fw.ExecutionContext } */
        private ServletExecutionContext context;
        /** ディスパッチ先クラス */
        private String dispatchingClass;
        /** HTTPリクエスト **/
        private HttpRequest request;
        /** HTTPレスポンス */
        private HttpResponse response;
        /** 開始日時 */
        private long startTime;
        /** 終了日時 */
        private long endTime;
        /** 最大メモリ量 */
        private long maxMemory;
        /** 空きメモリ量(開始時) */
        private long freeMemory;

        @SuppressWarnings("unchecked")
        private static final Map<String, Object> EMPTY_MAP = Collections.EMPTY_MAP;

        /**
         * {@link nablarch.fw.ExecutionContext}を設定する。
         * @param context {@link nablarch.fw.ExecutionContext}
         */
        public void setContext(ServletExecutionContext context) {
            this.context = context;
        }

        /**
         * サーブレットリクエストを返す。
         *
         * サーブレットコンテナ上で動作している場合は、サーブレットリクエストを返す。
         * そうでない場合(JUnit上で内蔵サーバーを使用せずにテストした場合など)は、
         * 実行時例外が送出される。
         *
         * @return サーブレットリクエスト
         * @throws ClassCastException
         *     サーブレットコンテナ上で動作していない場合。
         */
        public HttpServletRequest getServletRequest() throws ClassCastException {
            return context.getServletRequest();
        }
        /**
         * HTTPリクエストを設定する。
         * @param request HTTPリクエスト
         */
        public void setRequest(HttpRequest request) {
            this.request = request;
        }
        /**
         * HTTPレスポンスを設定する。
         * @param response HTTPレスポンス
         */
        public void setResponse(HttpResponse response) {
            this.response = response;
        }
        /**
         * セッションIDを取得する。
         * @return セッションID
         */
        public String getSessionId() {
            HttpSession session = getServletRequest().getSession(false);
            return session == null ? "" : session.getId();
        }
        /**
         * URLを取得する。
         * @return URL
         */
        public String getUrl() {
            return getServletRequest().getRequestURL().toString();
        }
        /**
         * クエリ文字列付きのURLを取得する。
         * @return クエリ文字列付きのURL
         */
        public String getRawUrl() {
            String queryString = getServletRequest().getQueryString();

            return queryString == null
                    ? getUrl()
                    : new StringBuilder(getUrl()).append("?").append(queryString).toString();
        }
        /**
         * ポート番号を取得する。
         * @return ポート番号
         */
        public int getPort() {
            return getServletRequest().getServerPort();

        }
        /**
         * HTTPメソッドを取得する。
         * @return HTTPメソッド
         */
        public String getMethod() {
            return request.getMethod();
        }
        /**
         * リクエストパラメータを取得する。
         * @return リクエストパラメータ
         */
        public Map<String, String[]> getParameters() {
            return request.getParamMap();
        }
        /**
         * セッションスコープマップを取得する。
         * @return セッションスコープマップ
         */
        public Map<String, Object> getSessionScopeMap() {
            return context.hasSession() ? context.getSessionScopeMap() : EMPTY_MAP;
        }
        /**
         * ディスパッチ先クラスを取得する。
         * @return ディスパッチ先クラス
         */
        public String getDispatchingClass() {
            return dispatchingClass;
        }
        /**
         * ディスパッチ先クラスを設定する。
         * @param dispatchingClass ディスパッチ先クラス
         */
        public void setDispatchingClass(String dispatchingClass) {
            this.dispatchingClass = dispatchingClass;
        }
        /**
         * クライアント端末IPアドレスを取得する。
         * @return クライアント端末IPアドレス
         */
        public String getClientIpAddress() {
            return getServletRequest().getRemoteAddr();
        }
        /**
         * クライアント端末ホストを取得する。
         * @return クライアント端末ホスト
         */
        public String getClientHost() {
            return getServletRequest().getRemoteHost();
        }
        /**
         * ステータスコードを取得する。
         * @return ステータスコード
         */
        public int getStatusCode() {
            if (response == null) {
                return -1;
            }
            return response.getStatusCode();
        }
        /**
         * コンテンツパスを取得する。
         * @return コンテンツパス
         */
        public String getContentPath() {
            if (response == null) {
                return "";
            }
            ResourceLocator locator = response.getContentPath();
            return locator != null ? locator.toString() : "";
        }
        /**
         * 開始日時を取得する。
         * @return 開始日時
         */
        public long getStartTime() {
            return startTime;
        }
        /**
         * 開始日時を設定する。
         * @param startTime 開始日時
         */
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        /**
         * 終了日時を取得する。
         * @return 終了日時
         */
        public long getEndTime() {
            return endTime;
        }
        /**
         * 終了日時を設定する。
         * @param endTime 終了日時
         */
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        /**
         * 実行時間を取得する。
         * @return 実行時間
         */
        public long getExecutionTime() {
            return endTime - startTime;
        }
        /**
         * 最大メモリ量を取得する。
         * @return 最大メモリ量
         */
        public long getMaxMemory() {
            return maxMemory;
        }
        /**
         * 最大メモリ量を設定する。
         * @param maxMemory 最大メモリ量
         */
        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }
        /**
         * 空きメモリ量(開始時)を取得する。
         * @return 空きメモリ量(開始時)
         */
        public long getFreeMemory() {
            return freeMemory;
        }
        /**
         * 空きメモリ量(開始時)を設定する。
         * @param freeMemory 空きメモリ量(開始時)
         */
        public void setFreeMemory(long freeMemory) {
            this.freeMemory = freeMemory;
        }
    }

    /**
     * リクエストIDを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class RequestIdItem implements LogItem<HttpAccessLogContext> {
        /**
         * リクエストIDを取得する。
         * @param context HttpAccessLogContext
         * @return リクエストID
         */
        public String get(HttpAccessLogContext context) {
            return ThreadContext.getRequestId();
        }
    }

    /**
     * ユーザIDを取得する。
     * @author Kiyohito Itoh
     */
    public static class UserIdItem implements LogItem<HttpAccessLogContext> {
        /**
         * ユーザIDを取得する。
         * @param context HttpAccessLogContext
         * @return ユーザID
         */
        public String get(HttpAccessLogContext context) {
            return ThreadContext.getUserId();
        }
    }

    /**
     * URLを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class UrlItem implements LogItem<HttpAccessLogContext> {
        /**
         * URLを取得する。
         * @param context HttpAccessLogContext
         * @return URL
         */
        public String get(HttpAccessLogContext context) {
            return context.getUrl();
        }
    }
    /**
     * クエリ文字列付きのURLを取得するクラス。
     * @author Yutaka Kanayama
     */
    public static class RawURLItem implements LogItem<HttpAccessLogContext> {
        /**
         * クエリ文字列付きのURLを取得する。
         * @param context HttpAccessLogContext
         * @return クエリ文字列付きのURL
         */
        public String get(HttpAccessLogContext context) {
            return context.getRawUrl();
        }
    }
    /**
     * ポート番号を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class PortItem implements LogItem<HttpAccessLogContext> {
        /**
         * ポート番号を取得する。
         * @param context HttpAccessLogContext
         * @return ポート番号
         */
        public String get(HttpAccessLogContext context) {
            return String.valueOf(context.getPort());
        }
    }
    /**
     * HTTPメソッドを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class MethodItem implements LogItem<HttpAccessLogContext> {
        /**
         * HTTPメソッドを取得する。
         * @param context HttpAccessLogContext
         * @return HTTPメソッド
         */
        public String get(HttpAccessLogContext context) {
            return context.getMethod();
        }
    }

    /**
     * リクエストパラメータを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ParametersItem extends MaskingMapItemSupport<HttpAccessLogContext> {
        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         * @param paramSeparator パラメータ間の区切り文字
         */
        public ParametersItem(char maskingChar, Pattern[] maskingPatterns, String paramSeparator) {
            super(maskingChar, maskingPatterns, paramSeparator);
        }
        /**
         * {@inheritDoc}}
         */
        protected Map<String, ?> getMap(HttpAccessLogContext context) {
            return context.getParameters();
        }
    }

    /**
     * セッションスコープ情報を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class SessionScopeItem extends MaskingMapItemSupport<HttpAccessLogContext> {
        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         * @param varSeparator 変数間の区切り文字
         */
        public SessionScopeItem(char maskingChar, Pattern[] maskingPatterns, String varSeparator) {
            super(maskingChar, maskingPatterns, varSeparator);
        }
        /**
         * {@inheritDoc}}
         */
        protected Map<String, ?> getMap(HttpAccessLogContext context) {
            return context.getSessionScopeMap();
        }
    }

    /**
     * ディスパッチ先クラスを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class DispatchingClassItem implements LogItem<HttpAccessLogContext> {
        /**
         * ディスパッチ先クラスを取得する。
         * @param context HttpAccessLogContext
         * @return ディスパッチ先クラス
         */
        public String get(HttpAccessLogContext context) {
            return context.getDispatchingClass();
        }
    }
    /**
     * セッションIDを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class SessionIdItem implements LogItem<HttpAccessLogContext> {
        /**
         * セッションIDを取得する。
         * @param context HttpAccessLogContext
         * @return セッションID
         */
        public String get(HttpAccessLogContext context) {
            return context.getSessionId();
        }
    }
    /**
     * ステータスコードを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StatusCodeItem implements LogItem<HttpAccessLogContext> {
        /**
         * ステータスコードを取得する。
         * @param context HttpAccessLogContext
         * @return ステータスコード
         */
        public String get(HttpAccessLogContext context) {
            int statusCode = context.getStatusCode();
            return statusCode != -1 ? String.valueOf(statusCode) : "";
        }
    }
    /**
     * クライアントへのレスポンスに使用するステータスコードを取得するクラス。
     * @author Koichi Asano
     */
    public static class ResponseStatusCodeItem implements LogItem<HttpAccessLogContext> {
        /**
         * ステータスコードを取得する。
         * @param context HttpAccessLogContext
         * @return ステータスコード
         */
        public String get(HttpAccessLogContext context) {
            int statusCode = HttpResponseUtil.chooseResponseStatusCode(context.response, context.context);
            return statusCode != -1 ? String.valueOf(statusCode) : "";
        }
    }
    /**
     * コンテンツパスを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ContentPathItem implements LogItem<HttpAccessLogContext> {
        /**
         * コンテンツパスを取得する。
         * @param context HttpAccessLogContext
         * @return コンテンツパス
         */
        public String get(HttpAccessLogContext context) {
            return context.getContentPath();
        }
    }
    /**
     * クライアント端末IPアドレスを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ClientIpAddressItem implements LogItem<HttpAccessLogContext> {
        /**
         * クライアント端末IPアドレスを取得する。
         * @param context HttpAccessLogContext
         * @return クライアント端末IPアドレス
         */
        public String get(HttpAccessLogContext context) {
            return context.getClientIpAddress();
        }
    }
    /**
     * クライアント端末ホストを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ClientHostItem implements LogItem<HttpAccessLogContext> {
        /**
         * クライアント端末ホストを取得する。
         * @param context HttpAccessLogContext
         * @return クライアント端末ホスト
         */
        public String get(HttpAccessLogContext context) {
            return context.getClientHost();
        }
    }
    /**
     * 開始日時を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StartTimeItem extends DateItemSupport<HttpAccessLogContext> {
        /**
         * コンストラクタ。
         * @param dateFormat 日時フォーマット
         */
        public StartTimeItem(DateFormat dateFormat) {
            super(dateFormat);
        }
        /** {@inheritDoc} */
        protected Date getDate(HttpAccessLogContext context) {
            return new Date(context.getStartTime());
        }
    }
    /**
     * 終了日時を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class EndTimeItem extends DateItemSupport<HttpAccessLogContext> {
        /**
         * コンストラクタ。
         * @param dateFormat 日時フォーマット
         */
        public EndTimeItem(DateFormat dateFormat) {
            super(dateFormat);
        }
        /** {@inheritDoc} */
        protected Date getDate(HttpAccessLogContext context) {
            return new Date(context.getEndTime());
        }
    }
    /**
     * 実行時間を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ExecutionTimeItem implements LogItem<HttpAccessLogContext> {
        /**
         * 実行時間を取得する。
         * @param context HttpAccessLogContext
         * @return 実行時間
         */
        public String get(HttpAccessLogContext context) {
            return String.valueOf(context.getExecutionTime());
        }
    }
    /**
     * 最大メモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class MaxMemoryItem implements LogItem<HttpAccessLogContext> {
        /**
         * 最大メモリ量を取得する。
         * @param context HttpAccessLogContext
         * @return 最大メモリ量
         */
        public String get(HttpAccessLogContext context) {
            return String.valueOf(context.getMaxMemory());
        }
    }
    /**
     * 空きメモリ量(開始時)を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class FreeMemoryItem implements LogItem<HttpAccessLogContext> {
        /**
         * 開始時の空きメモリ量を取得する。
         * @param context HttpAccessLogContext
         * @return 開始時の空きメモリ量
         */
        public String get(HttpAccessLogContext context) {
            return String.valueOf(context.getFreeMemory());
        }
    }
    /**
     * HTTPヘッダの User-Agent を取得する。
     *
     * @author Koichi Asano
     *
     */
    public static class ClientUserAgentItem implements LogItem<HttpAccessLogContext> {
        /**
         * HTTPヘッダの User-Agent を取得する。
         * @param context HTTPアクセスログコンテキスト
         * @return HTTPヘッダの User-Agent
         */
        public String get(HttpAccessLogContext context) {
            Object info = context.getServletRequest().getHeader("User-Agent");
            return info == null ? "" : (String) info;
        }
    }
}
