package nablarch.common.web.validator;

import java.lang.reflect.InvocationTargetException;

/**
 * リフレクションAPIを用いてFormのインスタンスを生成する{@link BeanValidationFormFactory}実装クラス。
 *
 * 本クラスは{@link BeanValidationFormFactory}のデフォルト実装である。
 * 本クラスを利用することで、5u15までと同じ動作となる。
 *
 * @author Taichi Uragami
 */
public class SimpleReflectionBeanValidationFormFactory implements BeanValidationFormFactory {

    @Override
    public <T> T create(Class<T> formClass) {
        try {
            return formClass.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(buildMessage(formClass), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(buildMessage(formClass), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(buildMessage(formClass), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(buildMessage(formClass), e);
        }
    }

    /**
     * 例外発生時のエラーメッセージを作成する。
     * @param formClass 生成対象のForm
     * @return エラーメッセージ
     */
    private String buildMessage(Class<?> formClass) {
        return "could not instantiate " + formClass + ".";
    }
}
