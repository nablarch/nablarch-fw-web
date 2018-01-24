package nablarch.common.web.token;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * @author Kiyohito Itoh
 */
public class RandomTokenGeneratorTest {

    @Test
    public void testGenerate() {
        
        RandomTokenGenerator generator = new RandomTokenGenerator();
        
        int size = 100;
        
        Pattern p = Pattern.compile("[0-9A-Za-z+/]{16}");
        Set<String> set = new HashSet<String>();
        
        for (int i = 0; i < size; i++) {
            String token = generator.generate();
            assertTrue(p.matcher(token).matches());
            set.add(token);
        }
        
        assertThat(set.size(), is(size));
    }
}
