package nablarch.common.web.handler.threadcontext;

import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

import java.util.TimeZone;

/**
 * HTTP上で選択されたタイムゾーンの保持を行う際に使用するユーティリティクラス。
 *
 * @see TimeZoneAttributeInHttpSupport
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public final class TimeZoneAttributeInHttpUtil {

    /** リポジトリから{@link TimeZoneAttributeInHttpSupport}を取得する際に使用するコンポーネント名。 */
    private static final String TIMEZONE_ATTRIBUTE_COMPONENT_NAME = "timeZoneAttribute";

    /** 隠蔽コンストラクタ。 */
    private TimeZoneAttributeInHttpUtil() {
    }

    /**
     * 指定されたタイムゾーンの保持とスレッドローカルへの設定を行う。
     * <p/>
     * 指定されたタイムゾーンがサポート対象のタイムゾーンでない場合は処理を行わない。
     * <p/>
     * サポート対象は、{@link nablarch.core.repository.SystemRepository}から取得した{@link TimeZoneAttributeInHttpSupport}で
     * サポートされているタイムゾーンとなる。
     * <p/>
     * タイムゾーンの保持については、アプリケーションで使用する
     * {@link TimeZoneAttributeInHttpSupport}のサブクラスのJavadocを参照。
     * 
     * @param request リクエスト
     * @param context 実行コンテキスト
     * @param timeZone タイムゾーン
     * @throws IllegalArgumentException リポジトリにサポート用コンポーネントが存在しなかった場合
     */
    @Published(tag = "architect")
    public static void keepTimeZone(HttpRequest request, ExecutionContext context, String timeZone) {
        TimeZoneAttributeInHttpSupport support = SystemRepository.get(TIMEZONE_ATTRIBUTE_COMPONENT_NAME);
        if (support == null) {
            throw new IllegalArgumentException(
                    "specified " + TIMEZONE_ATTRIBUTE_COMPONENT_NAME + " is not registered in SystemRepository.");
        }
        if (!support.isSupportedTimeZone(timeZone)) {
            return;
        }
        support.keepTimeZone(request, (ServletExecutionContext) context, timeZone);
        ThreadContext.setTimeZone(TimeZone.getTimeZone(timeZone));
    }
}
