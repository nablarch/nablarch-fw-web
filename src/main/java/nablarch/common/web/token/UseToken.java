package nablarch.common.web.token;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Interceptor;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * 二重サブミットを防止するために使用されるトークンを生成する{@link Interceptor}。
 * 
 * <p>
 * 本インターセプタで生成されたトークンは{@link OnDoubleSubmission}インターセプタでチェックされる。
 * 例えば、入力画面→確認画面→登録処理＆完了画面という画面構成の場合、
 * 確認画面を開くアクションでトークンを生成して登録処理のアクションでトークンのチェックを行うようにする。
 * </p>
 * 
 * <p>
 * 次にコード例を示す。
 * </p>
 * 
 * <pre>
 * {@code @UseToken}
 * public HttpResponse confirm(HttpRequest req, ExecutionContext ctx) {
 *     // 省略
 * }
 * 
 * {@code @OnDoubleSubmission(path = "xxx.jsp")}
 * public HttpResponse executeAndComplete(HttpRequest req, ExecutionContext ctx) {
 *     // 省略
 * }
 * </pre>
 * 
 * <p>
 * なお、ビューにJSPを使用している場合はn:formカスタムタグのuseToken属性をtrueにすることで、
 * 本インターセプタを適用しなくてもトークンを生成できる。
 * </p>
 * 
 * @author Taichi Uragami
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(UseToken.Impl.class)
@Published
public @interface UseToken {

    /**
     * {@link UseToken}のインターセプタ。
     * 
     * @author Taichi Uragami
     *
     */
    public static class Impl
            extends Interceptor.Impl<HttpRequest, HttpResponse, UseToken> {

        /**
         * トークンを生成して元のハンドラを実行する。
         */
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            TokenUtil.generateToken(((ServletExecutionContext) context).getServletRequest());
            return getOriginalHandler().handle(request, context);
        }
    }
}
