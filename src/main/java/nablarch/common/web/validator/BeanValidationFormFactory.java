package nablarch.common.web.validator;

/**
 * BeanValidation実行時に、Formクラスのインスタンスを生成するファクトリインタフェース。
 *
 * @author Taichi Uragami
 * @see BeanValidationStrategy
 */
public interface BeanValidationFormFactory {

    /**
     * 引数で指定されたFormの{@link Class}から、インスタンスを生成する。
     * @param formClass 生成対象となるFormのClass
     * @param <T> 生成されるフォームの型
     * @return 生成されたフォーム
     */
    <T> T create(Class<T> formClass);
}
