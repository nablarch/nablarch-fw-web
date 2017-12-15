package nablarch.common.web.validator;

import java.io.Serializable;

import nablarch.common.web.interceptor.InjectForm;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * リクエスト内容のバリデーション、オブジェクト生成ロジック。
 *
 * @author sumida
 */
public interface ValidationStrategy {

    /**
     * リクエスト内容をバリデーションし、エラーがなければ、{@link InjectForm}の<br>
     * form属性で指定された型のオブジェクトを生成して返す.
     *
     * @param request リクエスト
     * @param annotation InjectFormアノテーション
     * @param canValidate バリデーションメソッドの指定がある場合{@code true}
     * @param context 実行コンテキスト
     * 
     * @return バリデーション済みのオブジェクト
     */
    ValidationResult validate(HttpRequest request, InjectForm annotation, boolean canValidate,
            ServletExecutionContext context);

}
