package nablarch.common.web.compositekey;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nablarch.core.ThreadContext;
import nablarch.core.message.Message;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.creator.ReflectionFormCreator;
import nablarch.core.validation.validator.Required;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.message.MockStringResourceHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CompositeKeyConvertorTest {

    @Rule
    public SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("nablarch/common/web/compositekey/compositekey-test.xml");

    private CompositeKeyConvertor testee;
    private MockStringResourceHolder resource;

    private static final String[][] MESSAGES = {
        { "MSG00001", "ja", "{0}の入力が不正です。", "en", "input value of {0} was invalid." },
        { "PROP0001", "ja", "プロパティ1", "en", "property1" }, 
        };
    @Before
    public void setUp() {
        resource = repositoryResource.getComponentByType(MockStringResourceHolder.class);
        resource.setMessages(MESSAGES);
        testee = new CompositeKeyConvertor();
        testee.setConversionFailedMessageId("MSG00001");
    }

    private CompositeKeyType compositeKeyType = new CompositeKeyType() {

        public Class<? extends Annotation> annotationType() {
            return CompositeKeyType.class;
        }

        public int keySize() {
            return 3;
        }
    };

    private Required dummy = new Required() {

        public Class<? extends Annotation> annotationType() {
            return Required.class;
        }

        public String messageId() {
            return null;
        }
    };


    @Test
    public void testIsConvertible() {

        Map<String, String[]> params = new HashMap<String, String[]>();
        
        params.put("param", new String[]{"val1","val2", "val3"});
        
        ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                "", TestTarget.class, new ReflectionFormCreator(),
                params, "");
        
        assertTrue(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002,003"}, compositeKeyType));
        assertTrue(testee.isConvertible(context, "param", "PROP0001", null, compositeKeyType));

    }

    @Test
    public void testIsConvertibleFail() {

        Map<String, String[]> params = new HashMap<String, String[]>();
        
        params.put("param", new String[]{"val1","val2", "val3"});

        {
            ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                    "prop", TestTarget.class, new ReflectionFormCreator(),
                    params, "");
            
            // キー長が短い
            assertFalse(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002"}, compositeKeyType));
            
            List<Message> messages = context.getMessages();
            assertEquals(1, messages.size());
            ThreadContext.setLanguage(Locale.ENGLISH);
            assertEquals("input value of PROP0001 was invalid.", context.getMessages().get(0).formatMessage());
        }

        {
            ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                    "prop", TestTarget.class, new ReflectionFormCreator(),
                    params, "");
            
            // キー長が長い
            assertFalse(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002,003,004"}, compositeKeyType));
            
            List<Message> messages = context.getMessages();
            assertEquals(1, messages.size());
            ThreadContext.setLanguage(Locale.ENGLISH);
            assertEquals("input value of PROP0001 was invalid.", context.getMessages().get(0).formatMessage());
        }

        {
            ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                    "prop", TestTarget.class, new ReflectionFormCreator(),
                    params, "");
            
            // キーが複数
            assertFalse(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002,003", "001,001,001"}, compositeKeyType));
            
            List<Message> messages = context.getMessages();
            assertEquals(1, messages.size());
            ThreadContext.setLanguage(Locale.ENGLISH);
            assertEquals("input value of PROP0001 was invalid.", context.getMessages().get(0).formatMessage());
        }

        ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                "prop", TestTarget.class, new ReflectionFormCreator(),
                params, "");
        

        try {
            assertFalse(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002,003,004"}, null));
            fail("例外が発生するはず");
        } catch (IllegalArgumentException e) {
            
            assertEquals("annotation was not specified. conversion of nablarch.common.web.compositekey.CompositeKey" +
            		" requires annotation nablarch.common.web.compositekey.CompositeKeyType. propertyName = [param]"  , e.getMessage());
        }

        try {
            assertFalse(testee.isConvertible(context, "param", "PROP0001", new String[]{"001,002,003,004"}, dummy));
            fail("例外が発生するはず");
        } catch (IllegalArgumentException e) {
            assertEquals("illegal annotation type was specified." +
            		" conversion of nablarch.common.web.compositekey.CompositeKey" +
                        " requires annotation nablarch.common.web.compositekey.CompositeKeyType. propertyName = [param]", e.getMessage());
        }
        
    }
    
    @Test
    public void testConvert() {

        Map<String, String[]> params = new HashMap<String, String[]>();
        
        params.put("param", new String[]{"val1","val2", "val3"});

        ValidationContext<TestTarget> context = new ValidationContext<TestTarget>(
                "prop", TestTarget.class,
                new ReflectionFormCreator(), params, "");
        
        {
            CompositeKey converted = (CompositeKey) testee.convert(context, "test", new String[]{"001,002,003"}, compositeKeyType);
            assertThat(converted.getKeys(), is(new String[]{"001", "002" ,"003"}));
        }
        
        {
            CompositeKey converted = (CompositeKey) testee.convert(context, "test", null, compositeKeyType);
            assertNull(converted);
        }
    }
}
