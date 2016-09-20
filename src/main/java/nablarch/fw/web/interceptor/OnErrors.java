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
 * この{@link Interceptor}は、複数例外に対してレスポンスを指定したい場合に使用する。
 * 
 * 次の例では、"ApplicationException"が送出された場合の遷移先を入力画面(inputForm.jsp)、
 * "OptimisticLockException"が送出された場合の遷移先を業務トップ画面(topForm.jsp)、に設定している。
 * 
 * {@code @OnErrors} ({
 *      {@code @OnError} (type = OptimisticLockException.class, path ="servlet://topForm.jsp"),
 *      {@code @OnError} (type = ApplicationException.class, path ="servlet://inputForm.jsp")
 * })
 * public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *     updateUser(req.getParamMap());
 *     return new HttpResponse(200, "servlet://updatingCompleted.jsp");
 * }
 *  
 * この処理は、以下のコードによる処理と本質的に同等である。
 *  
 * public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *     try {
 *         updateUser(req.getParamMap());
 *         return new HttpResponse(200, "servlet://updatingCompleted.jsp");
 *     } catch(OptimisticLockException ole) {
 *         throw new HttpErrorResponse(400, "servlet://topForm.jsp", ole);
 *     } catch(ApplicationException ae) {
 *         throw new HttpErrorResponse(400, "servlet://inputForm.jsp", ae);
 *     }
 * }
 * 
 * OnErrorsアノテーションは、OnErrorアノテーションの定義順(上から順)に例外を処理する。
 * たとえば、上記の例では、OptimisticLockExceptionはApplicationExceptionのサブクラスなので、
 * 必ずApplicationExceptionの上に定義しなければ正常に処理が行われない。
 * </pre>
 * @author Kiyohito Itoh
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(OnErrors.Impl.class)
@Published
public @interface OnErrors {
    
    /**
     * このインターセプタがハンドリングするOnErrorアノテーション。
     * (必須属性)
     */
    OnError[] value();
    
    /**
     * {@link OnErrors}インターセプタの実装。
     * 
     * @author Kiyohito Itoh
     */
    public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, OnErrors> {
        
        /**
         * {@inheritDoc}
         * <pre>
         * このクラスの実装では、以下の処理を行う。
         *   1.  {@link OnErrors}アノテーションが付与されたhandleメソッドを実行する。
         *   2a. 1.の結果、{@link RuntimeException}が発生した場合は次の処理を行う。
         *       {@link OnErrors#value()}に指定された順に、発生した例外クラスが{@link OnError#type()}にマッチするか調べる。
         *       マッチした場合は、{@link OnError#statusCode()}と{@link OnError#path()}を使用して{@link HttpErrorResponse}を送出する。
         *       マッチしない場合は、発生した例外をそのまま送出する。
         *   2b. 1.の結果、{@link RuntimeException}が発生しない場合は、1.の結果をそのまま返却する。
         * </pre>
         */
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            try {
                return getOriginalHandler().handle(request, context);
            } catch (RuntimeException e) {
                for (OnError onError : getInterceptor().value()) {
                    if (onError.type().isAssignableFrom(e.getClass())) {
                        throw new HttpErrorResponse(onError.statusCode(), onError.path(), e);
                    }
                }
                throw e;
            }
        }
    }
}
