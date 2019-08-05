package nablarch.common.web.validator;

import java.lang.reflect.InvocationTargetException;

public class SimpleRefrectionBeanValidationFormFactory implements BeanValidationFormFactory {

    @Override
    public <T> T create(Class<T> formClass) {
        try {
            return formClass.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
