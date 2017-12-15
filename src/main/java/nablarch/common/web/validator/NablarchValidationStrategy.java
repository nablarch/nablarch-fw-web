package nablarch.common.web.validator;

import java.io.Serializable;

import nablarch.common.web.interceptor.InjectForm;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * NablarchのFormを使用する場合のリクエスト内容のバリデーション、オブジェクト(Form)生成ロジック.
 *
 * {@link BeanValidationStrategy}とは異なり、
 * 本実装ではバリデーションエラーが発生した場合には、
 * リクエストパラメータから値をコピーしたオブジェクト(Form)は
 * {@link ValidationResult}に格納されない。
 * これは、Nablarchのバリデーションが、（バリデーション前ではなく）
 * バリデーション成功後にオブジェクトを生成するためである。
 *
 * @author sumida
 * @see InjectForm.Impl#handle(HttpRequest, ExecutionContext)
 */
public class NablarchValidationStrategy implements ValidationStrategy {

    public ValidationResult validate(
            HttpRequest request, InjectForm annotation, boolean canValidate, final ServletExecutionContext context) {

        if (!canValidate) {
            return ValidationResult.createValidResult(null);
        }

        ValidationContext<? extends Serializable> validationContext = ValidationUtil.validateAndConvertRequest(
                    annotation.prefix(), annotation.form(), request, annotation.validate());
        if (!validationContext.isValid()) {
            return ValidationResult.createInvalidResult(validationContext.getMessages());
        }
        return ValidationResult.createValidResult(validationContext.createObject());
    }

}
