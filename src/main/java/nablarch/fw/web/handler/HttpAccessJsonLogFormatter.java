package nablarch.fw.web.handler;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogUtil.MapValueEditor;
import nablarch.core.log.LogUtil.MaskingMapValueEditor;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.log.app.JsonLogFormatterSupport;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.StringUtil;

import java.util.*;
import java.util.regex.Pattern;

/**
 * HTTPアクセスログのメッセージをフォーマットするクラス。
 * @author Shuji Kitamura
 */
public class HttpAccessJsonLogFormatter extends HttpAccessLogFormatter {

    /** リクエストIDの項目名 */
    private static final String TARGET_NAME_REQUEST_ID = "requestId";
    /** ユーザIDの項目名 */
    private static final String TARGET_NAME_USER_ID = "userId";
    /** URLの項目名 */
    private static final String TARGET_NAME_URL = "url";
    /** クエリ文字列の項目名 */
    private static final String TARGET_NAME_QUERY = "queryString";
    /** ポート番号の項目名 */
    private static final String TARGET_NAME_PORT = "port";
    /** HTTPメソッドの項目名 */
    private static final String TARGET_NAME_METHOD = "method";
    /** リクエストパラメータの項目名 */
    private static final String TARGET_NAME_PARAMETERS = "parameters";
    /** セッションスコープ情報の項目名 */
    private static final String TARGET_NAME_SESSION_SCOPE = "sessionScope";
    /** ディスパッチ先クラスの項目名 */
    private static final String TARGET_NAME_DISPATCHING_CLASS = "dispatchingClass";
    /** セッションIDの項目名 */
    private static final String TARGET_NAME_SESSION_ID = "sessionId";
    /** ステータスコードの項目名 */
    private static final String TARGET_NAME_STATUS_CODE = "statusCode";
    /** クライアントへのレスポンスに使用するステータスコードの項目名 */
    private static final String TARGET_NAME_RESPONSE_STATUS_CODE = "responseStatusCode";
    /** コンテンツパスの項目名 */
    private static final String TARGET_NAME_CONTENT_PATH = "contentPath";
    /** クライアント端末IPアドレスの項目名 */
    private static final String TARGET_NAME_CLIENT_IP_ADDRESS = "clientIpAddress";
    /** クライアント端末ホストの項目名 */
    private static final String TARGET_NAME_CLIENT_HOST = "clientHost";
    /** HTTPヘッダのUser-Agentの項目名 */
    private static final String TARGET_NAME_CLIENT_USER_AGENT = "clientUserAgent";
    /** 開始日時の項目名 */
    private static final String TARGET_NAME_START_TIME = "startTime";
    /** 終了日時の項目名 */
    private static final String TARGET_NAME_END_TIME = "endTime";
    /** 実行時間の項目名 */
    private static final String TARGET_NAME_EXECUTION_TIME = "executionTime";
    /** 最大メモリ量の項目名 */
    private static final String TARGET_NAME_MAX_MEMORY = "maxMemory";
    /** 空きメモリ量(開始時)の項目名 */
    private static final String TARGET_NAME_FREE_MEMORY = "freeMemory";

    /** リクエスト処理開始時の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_BEGIN_TARGETS = PROPS_PREFIX + "beginTargets";
    /** hiddenパラメータ復号後の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_PARAMETERS_TARGETS = PROPS_PREFIX + "parametersTargets";
    /** ディスパッチ先クラス決定後の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_DISPATCHING_CLASS_TARGETS = PROPS_PREFIX + "dispatchingClassTargets";
    /** リクエスト処理終了時の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_END_TARGETS = PROPS_PREFIX + "endTargets";

    /** デフォルトのリクエスト処理開始時のフォーマット */
    private static final String DEFAULT_BEGIN_TARGETS = "requestId,userId,sessionId,url,"
            + "method,port,clientIpAddress,clientHost";
    /** デフォルトのhiddenパラメータ復号後のフォーマット */
    private static final String DEFAULT_PARAMETERS_TARGETS = "parameters";
    /** デフォルトのディスパッチ先クラス決定後のフォーマット */
    private static final String DEFAULT_DISPATCHING_CLASS_TARGETS = "dispatchingClass";
    /** デフォルトのリクエスト処理終了時のフォーマット */
    private static final String DEFAULT_END_TARGETS = "requestId,userId,sessionId,url,"
            + "statusCode,contentPath,startTime,endTime,executionTime,maxMemory,freeMemory";

    /** リクエスト処理開始時のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<HttpAccessLogContext>> beginStructuredTargets;
    /** hiddenパラメータ復号後のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<HttpAccessLogContext>> parametersStructuredTargets;
    /** ディスパッチ先クラス決定後のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<HttpAccessLogContext>> dispatchingClassStructuredTargets;
    /** リクエスト処理終了時のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<HttpAccessLogContext>> endStructuredTargets;

    /** 出力対象にメモリ項目が含まれているか否か。 */
    private boolean containsMemoryItem;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private JsonLogFormatterSupport support;

    /**
     * 初期化。
     * フォーマット済みのログ出力項目を初期化する。
     * @param props 各種ログ出力の設定情報
     */
    @Override
    protected void initialize(Map<String, String> props) {
        initializeEnabled(props);

        support = new JsonLogFormatterSupport(
                new JsonSerializationSettings(props, PROPS_PREFIX, AppLogUtil.getFilePath()));

        Map<String, JsonLogObjectBuilder<HttpAccessLogContext>> objectBuilders = getObjectBuilders(props);
        containsMemoryItem = false;

        if (isBeginOutputEnabled()) {
            beginStructuredTargets = getStructuredTargets(objectBuilders, props, PROPS_BEGIN_TARGETS, DEFAULT_BEGIN_TARGETS);
        }

        if (isParametersOutputEnabled()) {
            parametersStructuredTargets = getStructuredTargets(objectBuilders, props, PROPS_PARAMETERS_TARGETS, DEFAULT_PARAMETERS_TARGETS);
        }

        if (isDispatchingClassOutputEnabled()) {
            dispatchingClassStructuredTargets = getStructuredTargets(objectBuilders, props, PROPS_DISPATCHING_CLASS_TARGETS, DEFAULT_DISPATCHING_CLASS_TARGETS);
        }

        if (isEndOutputEnabled()) {
            endStructuredTargets = getStructuredTargets(objectBuilders, props, PROPS_END_TARGETS, DEFAULT_END_TARGETS);
        }
    }

    /**
     * フォーマット対象のログ出力項目を取得する。
     * @param props 各種ログ出力の設定情報
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, JsonLogObjectBuilder<HttpAccessLogContext>> getObjectBuilders(Map<String, String> props) {

        Map<String, JsonLogObjectBuilder<HttpAccessLogContext>> objectBuilders
                = new HashMap<String, JsonLogObjectBuilder<HttpAccessLogContext>>();

        char maskingChar = getMaskingChar(props);
        Pattern[] maskingPatterns = getMaskingPatterns(props);
        MapValueEditor mapValueEditor = new MaskingMapValueEditor(maskingChar, maskingPatterns);

        objectBuilders.put(TARGET_NAME_REQUEST_ID, new RequestIdBuilder());
        objectBuilders.put(TARGET_NAME_USER_ID, new UserIdBuilder());
        objectBuilders.put(TARGET_NAME_URL, new UrlBuilder());
        objectBuilders.put(TARGET_NAME_QUERY, new QueryStringBuilder());
        objectBuilders.put(TARGET_NAME_PORT, new PortBuilder());
        objectBuilders.put(TARGET_NAME_METHOD, new MethodBuilder());
        objectBuilders.put(TARGET_NAME_PARAMETERS, new ParametersBuilder(mapValueEditor));
        objectBuilders.put(TARGET_NAME_SESSION_SCOPE, new SessionScopeBuilder(mapValueEditor));
        objectBuilders.put(TARGET_NAME_DISPATCHING_CLASS, new DispatchingClassBuilder());
        objectBuilders.put(TARGET_NAME_SESSION_ID, new SessionIdBuilder());
        objectBuilders.put(TARGET_NAME_STATUS_CODE, new StatusCodeBuilder());
        objectBuilders.put(TARGET_NAME_RESPONSE_STATUS_CODE, new ResponseStatusCodeBuilder());

        objectBuilders.put(TARGET_NAME_CONTENT_PATH, new ContentPathBuilder());
        objectBuilders.put(TARGET_NAME_CLIENT_IP_ADDRESS, new ClientIpAddressBuilder());
        objectBuilders.put(TARGET_NAME_CLIENT_HOST, new ClientHostBuilder());
        objectBuilders.put(TARGET_NAME_CLIENT_USER_AGENT, new ClientUserAgentBuilder());
        objectBuilders.put(TARGET_NAME_START_TIME, new StartTimeBuilder());
        objectBuilders.put(TARGET_NAME_END_TIME, new EndTimeBuilder());
        objectBuilders.put(TARGET_NAME_EXECUTION_TIME, new ExecutionTimeBuilder());
        objectBuilders.put(TARGET_NAME_MAX_MEMORY, new MaxMemoryBuilder());
        objectBuilders.put(TARGET_NAME_FREE_MEMORY, new FreeMemoryBuilder());

        return objectBuilders;
    }

    /**
     * フォーマット済みのログ出力項目を取得する。
     * @param objectBuilders オブジェクトビルダー
     * @param props 各種ログ出力の設定情報
     * @param targetsPropName 出力項目のプロパティ名
     * @param defaultTargets デフォルトの出力項目
     * @return フォーマット済みのログ出力項目
     */
    private List<JsonLogObjectBuilder<HttpAccessLogContext>> getStructuredTargets(
            Map<String, JsonLogObjectBuilder<HttpAccessLogContext>> objectBuilders,
            Map<String, String> props,
            String targetsPropName, String defaultTargets) {

        String targetsStr = props.get(targetsPropName);
        if (StringUtil.isNullOrEmpty(targetsStr)) targetsStr = defaultTargets;

        List<JsonLogObjectBuilder<HttpAccessLogContext>> structuredTargets
                = new ArrayList<JsonLogObjectBuilder<HttpAccessLogContext>>();

        String[] targets = targetsStr.split(",");
        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);
                if (objectBuilders.containsKey(key)) {
                    JsonLogObjectBuilder<HttpAccessLogContext> objectBuilder = objectBuilders.get(key);
                    if (objectBuilder instanceof MaxMemoryBuilder
                        || objectBuilder instanceof FreeMemoryBuilder) {
                        containsMemoryItem = true;
                    }
                    structuredTargets.add(objectBuilder);
                } else {
                    throw new IllegalArgumentException(
                            String.format("[%s] is unknown target. property name = [%s]", key, targetsPropName));
                }
            }
        }

        return structuredTargets;
    }

    /**
     * 出力対象にメモリ項目が含まれているか否かを判定する。
     * @return 出力対象にメモリ項目が含まれている場合はtrue
     */
    @Override
    public boolean containsMemoryItem() {
        return containsMemoryItem;
    }

    /**
     * リクエスト処理開始時のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    @Override
    public String formatBegin(HttpAccessLogContext context) {
        return support.getStructuredMessage(beginStructuredTargets, context);
    }

    /**
     * hiddenパラメータ復号後のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    @Override
    public String formatParameters(HttpAccessLogContext context) {
        return support.getStructuredMessage(parametersStructuredTargets, context);
    }

    /**
     * ディスパッチ先クラス決定後のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    @Override
    public String formatDispatchingClass(HttpAccessLogContext context) {
        return support.getStructuredMessage(dispatchingClassStructuredTargets, context);
    }

    /**
     * リクエスト処理終了時のメッセージをフォーマットする。
     * @param context HttpAccessLogContext
     * @return フォーマット済みのメッセージ
     */
    @Override
    public String formatEnd(HttpAccessLogContext context) {
        return support.getStructuredMessage(endStructuredTargets, context);
    }

    /**
     * リクエストIDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class RequestIdBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_REQUEST_ID, ThreadContext.getRequestId());
        }
    }

    /**
     * ユーザIDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class UserIdBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_USER_ID, ThreadContext.getUserId());
        }
    }

    /**
     * URLを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class UrlBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_URL, context.getUrl());
        }
    }

    /**
     * クエリ文字列を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class QueryStringBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_QUERY, context.getQueryString());
        }
    }

    /**
     * ポート番号を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class PortBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_PORT, context.getPort());
        }
    }

    /**
     * HTTPメソッドを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MethodBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_METHOD, context.getMethod());
        }
    }

    /**
     * リクエストパラメータを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ParametersBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /** マップの値のマスキング */
        private final MapValueEditor mapValueEditor;

        /**
         * コンストラクタ。
         * @param mapValueEditor マップの値のマスキング
         */
        public ParametersBuilder(MapValueEditor mapValueEditor) {
            this.mapValueEditor = mapValueEditor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            Map<String, String[]> map = new HashMap<String, String[]>();
            for (Map.Entry<String, String[]> entry : context.getParameters().entrySet()) {
                String [] values = new String[entry.getValue().length];
                for (int i = 0; i < entry.getValue().length; i++) {
                    values[i] = mapValueEditor.edit(entry.getKey(), entry.getValue()[i]);
                }
                map.put(entry.getKey(), values);
            }
            structuredObject.put(TARGET_NAME_PARAMETERS, map);
        }
    }

    /**
     * セッションスコープ情報を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class SessionScopeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /** マップの値のマスキング */
        private final MapValueEditor mapValueEditor;

        /**
         * コンストラクタ。
         * @param mapValueEditor マップの値のマスキング
         */
        public SessionScopeBuilder(MapValueEditor mapValueEditor) {
            this.mapValueEditor = mapValueEditor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            Map<String, String> map = new HashMap<String, String>();
            for (Map.Entry<String, Object> entry : context.getSessionScopeMap().entrySet()) {
                String values = mapValueEditor.edit(entry.getKey(), entry.getValue());
                map.put(entry.getKey(), values);
            }
            structuredObject.put(TARGET_NAME_SESSION_SCOPE, map);
        }
    }

    /**
     * ディスパッチ先クラスを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class DispatchingClassBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_DISPATCHING_CLASS, context.getDispatchingClass());
        }
    }

    /**
     * セッションIDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class SessionIdBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_SESSION_ID, context.getSessionId());
        }
    }

    /**
     * ステータスコードを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class StatusCodeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            int statusCode = context.getStatusCode();
            structuredObject.put(TARGET_NAME_STATUS_CODE, statusCode != -1 ? statusCode : null);
        }
    }

    /**
     * クライアントへのレスポンスに使用するステータスコードを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ResponseStatusCodeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            int statusCode = HttpResponseUtil.chooseResponseStatusCode(context.getResponse(), context.getContext());
            structuredObject.put(TARGET_NAME_RESPONSE_STATUS_CODE, statusCode != -1 ? statusCode : null);
            // (coverage) statusCodeが-1となるケースはない

        }
    }

    /**
     * コンテンツパスを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ContentPathBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_CONTENT_PATH, context.getContentPath());
        }
    }

    /**
     * クライアント端末IPアドレスを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ClientIpAddressBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_CLIENT_IP_ADDRESS, context.getClientIpAddress());
        }
    }

    /**
     * クライアント端末ホストを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ClientHostBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_CLIENT_HOST, context.getClientHost());
        }
    }

    /**
     * HTTPヘッダのUser-Agentを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ClientUserAgentBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_CLIENT_USER_AGENT, context.getServletRequest().getHeader("User-Agent"));
        }
    }

    /**
     * 開始日時を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class StartTimeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_START_TIME,  new Date(context.getStartTime()));
        }
    }

    /**
     * 終了日時を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class EndTimeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_END_TIME, new Date(context.getEndTime()));
        }
    }

    /**
     * 実行時間を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ExecutionTimeBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_EXECUTION_TIME, context.getExecutionTime());
        }
    }

    /**
     * 最大メモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MaxMemoryBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_MAX_MEMORY, context.getMaxMemory());
        }
    }

    /** 空きメモリ量(開始時)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class FreeMemoryBuilder implements JsonLogObjectBuilder<HttpAccessLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, HttpAccessLogContext context) {
            structuredObject.put(TARGET_NAME_FREE_MEMORY, context.getFreeMemory());
        }
    }
}