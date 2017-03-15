package nablarch.core.util.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class CopyOnReadMapTest {

    @Test
    public void testMakeSnapshotOfObjectGraphConsistsOfListsAndMaps()
    throws Exception {
        final List<String> list = new ArrayList<String>();
        list.add("listItem1");
        list.add("listItem2");
        list.add("listItem3");

        final Map<String, String> map = new HashMap<String, String>();
        map.put("entryKey1", "entryVal1");
        map.put("entryKey2", "entryVal2");
        map.put("entryKey3", "entryVal3");

        final Map<String, Object> data = new CopyOnReadMap<String, Object>(
            new HashMap<String, Object>() {{
                put("str" , "StrValue");
                put("list", list);
                put("map" , map);
            }}
        );

        final Thread mutateThread = new Thread("mutateThread") {
            @Override public void run() {
                data.put("str", "StrValue@updated");
                List<String> list = (List<String>) data.get("list");
                list.add("listItem4");
                Map<String, String> map = (Map<String, String>) data.get("map");
                map.clear();

                ((CopyOnReadMap<String, Object>) data).save();
            }
        };

        mutateThread.start();
        mutateThread.join();

        assertEquals(3, data.size());
        assertEquals("StrValue@updated", data.get("str"));
        List<String> listValue = (List<String>) data.get("list");
        assertEquals(4, listValue.size());
        assertEquals("listItem4", listValue.get(3));

        Map<String, String> mapValue = (Map<String, String>) data.get("map");
        assertTrue(mapValue.isEmpty());
    }

    @Test
    public void testConcurrentRead() throws Exception {
        final Map<String, String> data = new CopyOnReadMap<String, String>(
            new HashMap<String, String>() {{
                put("var1", "val1");
                put("var2", "val2");
                put("var3", "val3");
                put("ignoredVar1", "ignoredVal1");
            }}
        ).setIgnoredEntries("ignoredVar1", "ignoredVar2");

        final CountDownLatch initializationCompleted = new CountDownLatch(10);

        final CountDownLatch startLineOfUpdate = new CountDownLatch(1);
        final CountDownLatch updateCompleted   = new CountDownLatch(10);

        final CountDownLatch startLineOfSaving = new CountDownLatch(1);
        final CountDownLatch savingCompleted   = new CountDownLatch(10);

        final Vector<String> winners = new Vector<String>();
        final Vector<String> loosers = new Vector<String>();

        for (int i=0; i < 10; i++) {
            String threadName = "thread" + i;
            Thread thread = new Thread(threadName) {
                public void run() {

                    initializationCompleted.countDown();

                    try {
                        startLineOfUpdate.await();
                        // update the map.
                        for (Entry<String, String> entry : data.entrySet()) {
                            String oldVal = data.put(
                                entry.getKey(),
                                entry.getValue() + "@" + getName()
                            );
                            assertEquals(4, oldVal.length());
                        }
                    } catch (InterruptedException e) {
                        fail();
                    } finally {
                        updateCompleted.countDown();
                    }

                    try {
                        startLineOfSaving.await();
                        assertEquals("val1@" + getName(), data.get("var1"));
                        assertEquals("val2@" + getName(), data.get("var2"));
                        assertEquals("val3@" + getName(), data.get("var3"));
                        assertFalse(data.containsKey("ignoredVar1"));
                        assertFalse(data.containsKey("ignoredVar2"));

                        // save the changes.
                        ((CopyOnReadMap<String, String>) data).save();
                        winners.add(getName());

                    } catch (ConcurrentModificationException e) {
                        loosers.add(getName());
                    } catch (InterruptedException e) {
                        fail();
                    } finally {
                        savingCompleted.countDown();
                    }
                }
            };
            thread.start();
        }

        initializationCompleted.await();

        assertEquals("val1", data.get("var1"));
        assertEquals("val2", data.get("var2"));
        assertEquals("val3", data.get("var3"));
        assertFalse(data.containsKey("ignoredVar1"));
        assertFalse(data.containsKey("ignoredVar2"));

        startLineOfUpdate.countDown();
        updateCompleted.await();

        assertEquals("val1", data.get("var1"));
        assertEquals("val2", data.get("var2"));
        assertEquals("val3", data.get("var3"));
        assertFalse(data.containsKey("ignoredVar1"));
        assertFalse(data.containsKey("ignoredVar2"));

        startLineOfSaving.countDown();
        savingCompleted.await();

        assertEquals(1, winners.size());
        assertEquals(9, loosers.size());

        String winner = winners.get(0);

        assertEquals("val1", data.get("var1"));
        assertEquals("val2", data.get("var2"));
        assertEquals("val3", data.get("var3"));
        assertFalse(data.containsKey("ignoredVar1"));
        assertFalse(data.containsKey("ignoredVar2"));

        ((CopyOnReadMap<String, String>) data).refresh() ;

        assertEquals("val1" + "@" + winner, data.get("var1"));
        assertEquals("val2" + "@" + winner, data.get("var2"));
        assertEquals("val3" + "@" + winner, data.get("var3"));
        assertFalse(data.containsKey("ignoredVar1"));
        assertFalse(data.containsKey("ignoredVar2"));
    }
}
