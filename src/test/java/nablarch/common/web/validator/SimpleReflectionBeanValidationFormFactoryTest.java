package nablarch.common.web.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;

public class SimpleReflectionBeanValidationFormFactoryTest {


    private SimpleReflectionBeanValidationFormFactory sut = new SimpleReflectionBeanValidationFormFactory();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void Formのインスタンスが生成できること() {
        SampleForm sampleForm = sut.create(SampleForm.class);
        assertNotNull("インスタンスが生成されること。", sampleForm);
    }

    @Test
    public void Formのインスタンス生成に失敗したとき例外が発生すること() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("could not instantiate class nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactoryTest$PrivateForm.");

        sut.create(PrivateForm.class);
    }

    public static class SampleForm {
    }

    private static class PrivateForm {

    }
}