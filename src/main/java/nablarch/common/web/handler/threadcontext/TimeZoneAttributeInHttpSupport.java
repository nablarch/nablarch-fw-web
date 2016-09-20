package nablarch.common.web.handler.threadcontext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import nablarch.common.handler.threadcontext.TimeZoneAttribute;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Request;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTP上でタイムゾーンの保持を行うクラスの実装をサポートするクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class TimeZoneAttributeInHttpSupport extends TimeZoneAttribute {

    /** サポート対象のタイムゾーン */
    private Set<String> supportedTimeZones;

    /**
     * サポート対象のタイムゾーンを設定する。
     * @param supportedTimeZones サポート対象のタイムゾーン
     */
    public void setSupportedTimeZones(String... supportedTimeZones) {
        this.supportedTimeZones = new HashSet<String>(Arrays.asList(supportedTimeZones));
    }

    /**
     * サポート対象のタイムゾーンか否かを判定する。
     * @param timeZone タイムゾーン
     * @return サポート対象のタイムゾーンの場合はtrue
     */
    protected boolean isSupportedTimeZone(String timeZone) {
        return supportedTimeZones.contains(timeZone);
    }

    /**
     * コンテキストスレッドに格納するこのプロパティの値を返す。 
     * <p/>
     * {@link #getTimeZone(HttpRequest, ServletExecutionContext)}に処理を委譲する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象のタイムゾーン
     */
    public Object getValue(Request<?> req, ExecutionContext ctx) {
        return getTimeZone((HttpRequest) req, (ServletExecutionContext) ctx);
    }

    /**
     * スレッドコンテキストに保持するタイムゾーン属性を返す。
     * <pre>
     * このクラスの実装では、以下の処理を行う。
     * 
     * 
     * 1. 保持しているタイムゾーンの取得を試みる。({@link #getKeepingTimeZone(HttpRequest, ServletExecutionContext)})
     * 
     *   サポート対象のタイムゾーンが取得できた場合は、取得できたタイムゾーンを返す。
     *   
     *   サポート対象のタイムゾーンが取得できない場合は2.に進む。
     *   
     * 2.デフォルトのタイムゾーンを返す。(TimeZoneAttribute#getValue(Request, ExecutionContext))
     * 
     * </pre>
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象のタイムゾーン
     */
    protected TimeZone getTimeZone(HttpRequest req, ServletExecutionContext ctx) {

        String keepingTimeZone = getKeepingTimeZone(req, ctx);
        if (isSupportedTimeZone(keepingTimeZone)) {
            return TimeZone.getTimeZone(keepingTimeZone);
        }

        return (TimeZone) super.getValue(req, ctx);
    }

    /**
     * ユーザが選択したタイムゾーンを保持する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @param timeZone ユーザが選択したタイムゾーン
     */
    protected abstract void keepTimeZone(HttpRequest req, ServletExecutionContext ctx, String timeZone);

    /**
     * 保持しているタイムゾーンを取得する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return タイムゾーン。保持していない場合はnull
     */
    protected abstract String getKeepingTimeZone(HttpRequest req, ServletExecutionContext ctx);
}
