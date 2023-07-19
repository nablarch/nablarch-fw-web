package nablarch.common.web.interceptor;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.common.web.validator.NablarchValidationStrategy;
import nablarch.common.web.validator.ValidationStrategy;
import nablarch.core.beans.BeanUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Interceptor;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * フォームをリクエストスコープに設定する{@link Interceptor}。
 * <p>
 * 本インターセプタでは次の２つの機能を提供する。
 * <ul>
 *   <li>
 *     <a href="#processDetail">バリデーションや初期化処理を行ったフォームをリクエストスコープに設定する</a>
 *   </li>
 *   <li>
 *     <a href="#validationStrategy">指定されたバリデーションエンジンでバリデーションを行う</a>
 *   </li>
 * </ul>
 *
 * <h3 id="processDetail">バリデーションや初期化処理を行ったフォームをリクエストスコープに設定する</h3>
 * 本インターセプタは業務アクションハンドラに次のように実装する。<br>
 * <pre>
 *     {@code @InjectForm(form = UserForm.class, prefix = "form", validate = "register")}
 *     {@code @OnError(type = ApplicationException.class, path = "forward://registerForm.html")
 *     public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
 *
 *         UserForm form = ctx.getRequestScopedVar("form");
 *
 *         // 以下、省略
 *     }}
 * </pre>
 * 上記のような{@code @InjectForm}アノテーションが指定されたメソッドは、
 * メソッド実行前に以下の処理順でフォームが生成され、リクエストスコープに設定される。
 * <ol>
 *   <li>
 *     指定のvalidationStrategyに従って、バリデーションを実行し、フォームを生成する。
 *     <ul>
 *       <li>
 *         バリデーションエラーが発生した場合は{@link nablarch.core.message.ApplicationException}を送出する。
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     {@link InjectForm#initialize}属性が指定されていれば、初期化処理を実行する。
 *   </li>
 *   <ol>
 *     <li>
 *       デフォルトコンストラクタでフォームを生成する。
 *     </li>
 *     <li>
 *       指定の初期化メソッドを実行する。
 *     </li>
 *     <li>
 *       バリデーションを実行して生成したフォームから初期化したフォームへ値をコピーする。
 *     </li>
 *   </ol>
 *   <li>
 *     生成したフォームを{@link InjectForm#name}属性の名前でリクエストスコープに設定する。
 *   </li>
 * </ol>
 *
 * <h3 id="validationStrategy">指定されたバリデーションエンジンでバリデーションを行う</h3>
 * validationStrategyという名前でコンポーネントを定義することでバリデーションエンジンを指定できる。<br>
 * <pre>
 *     {@code //指定例 (Bean Validation)
 *     <component name="validationStrategy"
 *                class="nablarch.core.validation.ee.BeanValidationStrategy" />}
 * </pre>
 * デフォルトでは{@link NablarchValidationStrategy}が使用される。
 * </p>
 *
 * @author kawasima
 * @author Kiyohito Itoh
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(InjectForm.Impl.class)
@Published
public @interface InjectForm {

    /**
     * 対象のフォームクラス。（必須属性）
     */
    Class<? extends Serializable> form();

    /**
     * フォームに設定するパラメータのプレフィックス。（デフォルトは空文字）
     */
    String prefix() default "";

    /**
     * フォームをリクエストスコープに設定する際に使用する変数名。（デフォルトは"form"）
     */
    String name() default "form";

    /**
     * 初期化メソッド。
     * <pre>
     *     対象のフォームクラスに以下のシグネチャでインスタンスメソッドとして実装する。<br>
     *     {@code public void <メソッド名>(HttpRequest request, ExecutionContext context)}<br>
     *     初期化メソッドは、フォームの初期値（固定値または画面遷移時に復元する入力値）を設定する際に使用する。
     * </pre>
     */
    String initialize() default "";

    /**
     * バリデーション対象メソッド。
     * <pre>
     *     {@link nablarch.core.validation.ValidateFor}で設定した文字列を指定する。
     * </pre>
     */
    String validate() default "";

    /**
     * Bean Validationのグループ。
     */
    Class<?>[] validationGroup() default {};

    /**
     * {@link InjectForm}アノテーションのインターセプタ。
     * @author kawasima
     * @author Kiyohito Itoh
     */
    public static class Impl extends Interceptor.Impl<HttpRequest, HttpResponse, InjectForm> {

        /** バリデーションストラテジ */
        private ValidationStrategy validationStrategy = getValidationStrategy();

        /**
         * フォームを生成し、リクエストスコープに設定する。
         * @param request リクエスト
         * @param context 実行コンテキスト
         * @return レスポンス
         */
        @Override
        public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

            InjectForm annotation = getInterceptor();
            boolean canInitialize = StringUtil.hasValue(annotation.initialize());
            boolean canValidate = StringUtil.hasValue(annotation.validate());

            Serializable form = validationStrategy.validate(request, annotation, canValidate,
                    (ServletExecutionContext) context);

            if (canInitialize) {
                Serializable initForm = createForm(annotation);
                initializeForm(request, context, annotation, initForm);
                form = form != null ? BeanUtil.copyExcludesNull(form, initForm) : initForm;
            }

            if (form == null) {
                form = createForm(annotation);
            }

            context.setRequestScopedVar(annotation.name(), form);

            return getOriginalHandler().handle(request, context);
        }

        /**
         * フォームを生成する。
         * @param annotation InjectFormアノテーション
         * @return 生成したフォーム
         */
        private Serializable createForm(InjectForm annotation) {
            return newInstance(annotation.form());
        }

        /**
         * クラスのインスタンスを生成する。
         * @param <T> クラスの型
         * @param c クラス
         * @return インスタンス
         */
        private <T> T newInstance(Class<T> c) {
            try {
                return c.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "class instantiation failed. class = [" + c.getName() + "]", e);
            }
        }

        /**
         * フォームを初期化する。
         * @param request リクエスト
         * @param context 実行コンテキスト
         * @param annotation InjectFormアノテーション
         * @param form フォーム
         */
        private void initializeForm(HttpRequest request, ExecutionContext context, InjectForm annotation, Serializable form) {
            try {
                annotation.form().getMethod(annotation.initialize(), HttpRequest.class, ExecutionContext.class)
                                 .invoke(form, request, context);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "form initialization failed. form = [" + annotation.form().getName() + "], method = [" + annotation.initialize() + "]", e);
            }
        }

        /**
         * {@link ValidationStrategy}の具象クラスを返す.
         * <ul>
         * <li>
         * コンポーネント定義にvalidationStrategyが指定されていた場合、指定されたクラスを返す.
         * </li>
         * <li>
         * コンポーネント定義が指定されていない場合、デフォルトのStrategyとして{@link NablarchValidationStrategy}を返す.
         * </li>
         * </ul>
         * @return Strategyの具象クラス
         */
        private static synchronized ValidationStrategy getValidationStrategy() {
            ValidationStrategy validationStrategy = SystemRepository.get("validationStrategy");
            if (validationStrategy == null) {
                validationStrategy = new NablarchValidationStrategy();
            }
            return validationStrategy;
        }

    }
}
