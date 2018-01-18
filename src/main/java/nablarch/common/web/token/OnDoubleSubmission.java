package nablarch.common.web.token;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Interceptor;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * 二重サブミットを防止する{@link Interceptor}。
 * <p>
 * 業務アクションハンドラのメソッドに付与することで、二重サブミット(同一リクエストの二重送信)のチェックを行う。
 * </p>
 * 本インターセプタを使用するためには、jspでのn:formタグによるトークン設定が必要である。
 * <pre>
 *     {@code <n:form useToken="true">
 *     <n:submit type="button" value="Submit" uri="/XXXXX" allowDoubleSubmission="false">
 *     </n:form>}
 * </pre>
 * 本インターセプタは、業務アクションハンドラに次のように実装する。
 * <pre>
 *     {@code @OnDoubleSubmission(path = "XXX.jsp")}
 *     {@code @OnError(type = ApplicationException.class, path = "forward://XXX.html")
 *     public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *         // 省略
 *     }}
 * </pre>
 *
 * @author Kiyohito Itoh
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(OnDoubleSubmission.Impl.class)
@Published
public @interface OnDoubleSubmission {
    
    /**
     * 二重サブミットと判定した場合の遷移先のリソースパスを返す。<br>
     * 個別に遷移先を変更する場合に指定する。<br>
	 * リソースパスは必須項目であり、指定しなかった場合は{@link NullPointerException}が発生する。
     */
    String path() default "";
    
    /**
     * 二重サブミットと判定した場合の遷移先画面に表示するエラーメッセージに使用するメッセージIDを返す。<br>
     * 個別にメッセージIDを変更する場合に指定する。<br>
	 * デフォルトでは、アプリケーション設定一覧表_presetで設定したエラーメッセージが使用される。
     */
    String messageId() default "";
    
    /**
     * 二重サブミットと判定した場合のレスポンスステータスを返す。
     * 個別にレスポンスステータスを変更する場合に指定する。
	 * デフォルトでは、{@link BasicDoubleSubmissionHandler}のフィールドに定義されている400を返す。
     */
    int statusCode() default -1;
    
    /**
     * {@link OnDoubleSubmission}アノテーションのインターセプタ。<br>
     * トークンをチェックし、二重サブミットの場合は指定された画面遷移を行うための{@link HttpResponse}を返す。
     * @author Kiyohito Itoh
     */
    public static class Impl
    extends Interceptor.Impl<HttpRequest, HttpResponse, OnDoubleSubmission> {
        
        /** リポジトリから{@link DoubleSubmissionHandler}を取得できない場合に使用するデフォルトハンドラ */
        private static final DoubleSubmissionHandler DEFAULT_HANDLER = new BasicDoubleSubmissionHandler();
        
        /** {@link DoubleSubmissionHandler}をリポジトリから取得する際に使用する名前 */
        private static final String DOUBLE_SUBMISSION_HANDLER_NAME = "doubleSubmissionHandler";
        
        /**
         * {@inheritDoc}
         * <p>
         * {@link DoubleSubmissionHandler}を実装したクラスに処理を委譲する。
         * {@link DoubleSubmissionHandler}は"doubleSubmissionHandler"という名前でリポジトリから取得する。
         * リポジトリから{@link DoubleSubmissionHandler}を取得できない場合は、{@link BasicDoubleSubmissionHandler}を使用する。
         * </p>
         */
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            DoubleSubmissionHandler handler = (DoubleSubmissionHandler) SystemRepository.getObject(DOUBLE_SUBMISSION_HANDLER_NAME);
            if (handler == null) {
                handler = DEFAULT_HANDLER;
            }
            return handler.handle(request, context, getOriginalHandler(), getInterceptor());
        }
    }
}
