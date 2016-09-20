package nablarch.common.web.interceptor.sample.form;

import java.lang.annotation.Annotation;
import java.util.List;

import nablarch.core.validation.domain.DomainDefinition;
import nablarch.core.validation.domain.DomainValidationHelper;
import nablarch.core.validation.validator.Length;

public enum DomainType implements DomainDefinition {

    @Length(min=10, max=10)
    USER_ID;

    @Override
    public Annotation getConvertorAnnotation() {
        return DomainValidationHelper.getConvertorAnnotation(this);
    }

    @Override
    public List<Annotation> getValidatorAnnotations() {
        return DomainValidationHelper.getValidatorAnnotations(this);
    }
}
