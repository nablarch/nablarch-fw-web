package nablarch.fw.web.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Interceptor;
import nablarch.fw.web.handler.SessionConcurrentAccessHandler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * セッション変更の書き戻しに失敗した場合に実行時例外を送出し、
 * DBトランザクションをロールバックする。
 * <pre>
 * セッション変更の書き戻し処理が行われるのは、セッション同期ポリシーとして、
 * {@link SessionConcurrentAccessHandler.ConcurrentAccessPolicy#CONCURRENT}(デフォルト)が使用されている場合のみである。
 * (他のポリシーが使用している場合、このアノテーションは単に無視される。)
 * </pre>
 * 
 * @author Iwauo Tajima
 * @see SessionConcurrentAccessHandler
 * @see SessionConcurrentAccessHandler.ConcurrentAccessPolicy#CONCURRENT
 * @deprecated 標準のハンドラ構成では、本アノテーションを使用しても、DBトランザクションをロールバックすることができないため、
 *             本アノテーションを使用しないこと。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(ErrorOnSessionWriteConflict.Impl.class)
@Published
@Deprecated
public @interface ErrorOnSessionWriteConflict {
    /**
     * {@link ErrorOnSessionWriteConflict}インターセプタの処理内容を実装するリクエストハンドラ。
     * @author Iwauo Tajima
     */
    public static class Impl
    extends Interceptor.Impl<HttpRequest, HttpResponse, ErrorOnSessionWriteConflict> {
        /**{@inheritDoc}
         * <pre>
         * {@link SessionConcurrentAccessHandler}側の設定を一時的に変更し、
         * セッション変更の書き戻しに失敗した場合に実行時例外を送出して
         * DBトランザクションをロールバックするようにする。
         * </pre>
         */
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            SessionConcurrentAccessHandler.setThrowsErrorOnSessionWriteConflict(true);
            return getOriginalHandler().handle(req, ctx);
        }
    }
}
