package nablarch.common.web.compositekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link CompositeKey}クラス のテスト。
 *
 * @author Koichi Asano 
 *
 */
public class CompositeKeyTest {

    @Test
    public void testSame() {
        CompositeKey key1 = new CompositeKey("a", "b", "c");
        CompositeKey key2 = new CompositeKey("a", "b", "c");
        CompositeKey key3 = new CompositeKey("a", "b", "d");

        assertEquals(key1.hashCode(), key2.hashCode());
        assertTrue(key1.equals(key2));
        

        assertTrue(key1.hashCode() != key3.hashCode());
        assertTrue(key1.equals(key1));
        assertFalse(key1.equals(key3));
        assertFalse(key1.equals(null));
        assertFalse(key1.equals(new Object()));
    }
}
