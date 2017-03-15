package nablarch.core.util.map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link nablarch.core.util.map.EnumerableIterator}のテスト。
 *
 * @author hisaaki sioiri
 */
public class EnumerableIteratorTest {

    private EnumerableIterator<String> sut;

    @Before
    public void setUp() {
        sut = new EnumerableIterator<String>(Arrays.asList("1", "2").iterator());
    }
    @Test
    public void testHasMoreElements() {

        assertThat("要素あり", sut.hasMoreElements(), is(true));
        sut.next();
        assertThat("要素あり", sut.hasMoreElements(), is(true));
        sut.next();
        assertThat("最終要素までいったので残りの要素はなし", sut.hasMoreElements(), is(false));
    }

    @Test
    public void testNextElement() {
        assertThat(sut.nextElement(), is("1"));
        assertThat(sut.nextElement(), is("2"));
    }

    @Test
    public void testNext() {
        assertThat(sut.next(), is("1"));
        assertThat(sut.next(), is("2"));
    }

    @Test
    public void testHasNext() {
        assertThat("要素あり", sut.hasNext(), is(true));
        sut.next();
        assertThat("要素あり", sut.hasNext(), is(true));
        sut.next();
        assertThat("最終要素までいったので残りの要素はなし", sut.hasNext(), is(false));
    }

    @Test
    public void remove() {
        class IteratorSub implements Iterator<String> {
            public boolean remove = false;
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                return "";
            }

            @Override
            public void remove() {
                remove = true;
            }
        };
        IteratorSub iterator = new IteratorSub();
        EnumerableIterator<String> target = new EnumerableIterator<String>(iterator);

        assertThat(iterator.remove, is(false));
        target.remove();
        assertThat(iterator.remove, is(true));
    }
}
