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
 * <p>
 * {@link BeanValidationStrategy}とは異なり、
 * 本実装ではバリデーションエラーが発生した場合には、
 * リクエストパラメータから値をコピーしたオブジェクト(Form)を
 * リクエストスコープに格納する機能は持たない。
 * これは、Nablarchのバリデーションが、（バリデーション前ではなく）
 * バリデーション成功後にオブジェクトを生成するためである。
 * </p>
 *
 * @author sumida
 */
public class NablarchValidationStrategy implements ValidationStrategy {

    public Serializable validate(
            HttpRequest request, InjectForm annotation, boolean canValidate, final ServletExecutionContext context) {

        if (annotation.validationGroup().length != 0) {
            throw new IllegalArgumentException("validationGroup attribute cannot be specified when using NablarchValidationStrategy");
        }

        if (!canValidate) {
            return null;
        }

        ValidationContext<? extends Serializable> validationContext = ValidationUtil.validateAndConvertRequest(
                    annotation.prefix(), annotation.form(), request, annotation.validate());
        validationContext.abortIfInvalid();

        return validationContext.createObject();
    }

}
