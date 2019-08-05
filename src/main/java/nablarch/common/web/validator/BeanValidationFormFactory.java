package nablarch.common.web.validator;

public interface BeanValidationFormFactory {

    <T> T create(Class<T> formClass);
}
