package nablarch.common.web.validator;

import java.io.Serializable;

import nablarch.common.web.interceptor.InjectForm;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationUtil;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * NablarchのFormを使用する場合のリクエスト内容のバリデーション、オブジェクト(Form)生成ロジック.
 *
 * @author sumida
 */
public class NablarchValidationStrategy implements ValidationStrategy {

    public ValidationResult validate(
            HttpRequest request, InjectForm annotation, boolean canValidate, final ServletExecutionContext context) {

        if (!canValidate) {
            return ValidationResult.createValidResult(null);
        }

        ValidationContext<? extends Serializable> validationContext = ValidationUtil.validateAndConvertRequest(
                    annotation.prefix(), annotation.form(), request, annotation.validate());
        if (validationContext.isValid()) {
            return ValidationResult.createValidResult(validationContext.createObject());
        }
        return ValidationResult.createInvaidResult(validationContext.getMessages());
    }

}
