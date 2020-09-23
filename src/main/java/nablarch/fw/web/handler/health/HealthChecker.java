package nablarch.fw.web.handler.health;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;

/**
 * ヘルスチェックを行うクラス。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class HealthChecker {

    /** ヘルスチェックの対象を表す名前 */
    private String name;

    /**
     * ヘルスチェックの対象を表す名前を取得する。
     *
     * @return ヘルスチェックの対象を表す名前
     */
    public String getName() {
        return name;
    }

    /**
     * ヘルスチェックの対象を表す名前を設定する。
     *
     * @param name ヘルスチェックの対象を表す名前
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * ヘルスチェックを行う。
     *
     * {@link #tryOut(HttpRequest, ExecutionContext)}を呼び出し、その結果を返す。
     * {@link #tryOut(HttpRequest, ExecutionContext)}で例外が発生した場合はfalseを返す。
     *
     * @param request リクエスト
     * @param context コンテキスト
     * @return ヘルスチェックに成功した場合はtrue
     */
    public boolean check(HttpRequest request, ExecutionContext context) {
        try {
            return tryOut(request, context);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * ヘルシーと判断できる処理を試す。
     *
     * @param request リクエスト
     * @param context コンテキスト
     * @return ヘルシーな場合はtrue
     * @throws Exception 試した結果発生した例外
     */
    protected abstract boolean tryOut(HttpRequest request, ExecutionContext context) throws Exception;
}
