package nablarch.fw.web.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Interceptor;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * リクエストハンドラが例外を送出した場合のレスポンスを指定する{@link Interceptor}。
 * <pre>
 * 次の例では、"ApplicationException"が送出された場合の遷移先を
 * 入力画面(registerForm.jsp)に設定している。
 * 
 * {@code @OnError} (
 *      type = ApplicationException.class
 *    , path ="servlet://registerForm.jsp"
 *  )
 *  public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *      registerUser(req.getParamMap());
 *      return new HttpResponse(200, "servlet://registrationCompleted.jsp");
 *  }
 *  
 *  この処理は、以下のコードによる処理と本質的に同等である。
 *  
 *  public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *      try {
 *          registerUser(req.getParamMap());
 *          return new HttpResponse(200, "servlet://registrationCompleted.jsp");
 *      } catch(ApplicationException ae) {
 *          throw new HttpErrorResponse(400, "servlet://registerForm.jsp", ae);
 *      }
 *  }
 * </pre>
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 * @see OnError.Impl
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(OnError.Impl.class)
@Published
public @interface OnError {
    /**
     * このインターセプタがハンドリングする実行時例外。
     * (必須属性)
     */
    Class<? extends RuntimeException> type();

    /**
     * リクエストハンドラから、{@link #type}
     * に合致する実行時例外が送出された場合に送信する画面のリソースパスを返す。
     * (必須属性)
     */
    String path();

    /**
     * リクエストハンドラから、{@link #type}
     * に合致する実行時例外が送出された場合のレスポンスステータスを返す。
     * <pre>
     * デフォルトではステータスコード400(Bad Request)となる。
     * </pre>
     */
    int statusCode() default 400;

    /**
     * {@link OnError}インターセプタの実装。
     *
     * @author Iwauo Tajima <iwauo@tis.co.jp>
     * @see OnError
     */
    public static class Impl
    extends Interceptor.Impl<HttpRequest, HttpResponse, OnError> {
        /**
         * {@inheritDoc}
         * <pre>
         * このクラスの実装では、以下の処理を行う。
         *   1.  {@link OnError}アノテーションが付与されたhandleメソッドを実行する。
         *   2a. 1.の結果、{@link OnError#type}に指定された例外クラスが送出された場合は、
         *       statusCodeに指定されたステータスコード、pathに指定されたコンテンツパスの
         *       レスポンスエラー例外を送出する。
         *   2b. そうでない場合は、1.の結果をそのまま返却する。
         * 
         * {@link OnError}アノテーションの{@link OnError#type}に指定された実行時例外を捕捉し、
         * 同じく{@link OnError#path}に指定された画面へのレスポンスを作成して返す。
         * </pre>
         */
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            OnError onError = getInterceptor();
            Class<? extends Throwable> errorType = onError.type();

            try {
                return getOriginalHandler().handle(req, ctx);

            } catch (RuntimeException e) {
                if (errorType.isAssignableFrom(e.getClass())) {
                    throw new HttpErrorResponse(
                        onError.statusCode(), onError.path(), e
                    );
                }
                throw e;
            }
        }
    }
}
