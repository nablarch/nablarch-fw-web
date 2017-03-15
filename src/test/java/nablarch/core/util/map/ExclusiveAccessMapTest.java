package nablarch.core.util.map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Test;

public class ExclusiveAccessMapTest {
    
    @Test
    public void testExclusiveAccessTest() throws Exception {
        final Map<String, String> baseMap = new HashMap<String, String>(){{
            put("var1", "val1");
            put("var2", "val2");
            put("var3", "val3");
        }};
        final Map<String, String> map =
                new ExclusiveAccessMap<String, String>(baseMap);
        
        final CountDownLatch initAllCompleted = new CountDownLatch(2);
        final CountDownLatch startGate        = new CountDownLatch(1);
        final CountDownLatch finishedATask    = new CountDownLatch(1);
        final CountDownLatch finishedAllTask  = new CountDownLatch(2);
        final CountDownLatch goalGate         = new CountDownLatch(1);
        
        final List<Thread> taskFinishedThreads = new ArrayList<Thread>();
        
        Thread mutatorThreadA = new Thread("threadA") {
            public void run() {
                try {
                    initAllCompleted.countDown();
                    startGate.await();
                    
                    Thread.sleep(20);
                    map.put("var1", "val1@" + getName());
                    Thread.sleep(20);
                    map.put("var2", "val2@" + getName());
                    Thread.sleep(20);
                    map.put("var3", "val3@" + getName());
                    Thread.sleep(20);
                    taskFinishedThreads.add(this);
                    
                    finishedATask.countDown();
                    finishedAllTask.countDown();
                    goalGate.await();
                    ((ExclusiveAccessMap<String, String>) map).unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Thread mutatorThreadB = new Thread("threadB") {
            public void run() {
                try {
                    initAllCompleted.countDown();
                    startGate.await();
                    
                    Thread.sleep(20);
                    map.put("var1", "val1@" + getName());
                    Thread.sleep(20);
                    map.put("var2", "val2@" + getName());
                    Thread.sleep(20);
                    map.put("var3", "val3@" + getName());
                    Thread.sleep(20);
                    taskFinishedThreads.add(this);
                    
                    finishedATask.countDown();
                    finishedAllTask.countDown();
                    goalGate.await();
                    ((ExclusiveAccessMap<String, String>) map).unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        mutatorThreadA.start();
        mutatorThreadB.start();

        initAllCompleted.await();
        startGate.countDown();
        finishedATask.await();
        
        Assert.assertEquals(1, taskFinishedThreads.size());
        String winnerName = taskFinishedThreads.get(0).getName();
        Assert.assertEquals("val1@" + winnerName, baseMap.get("var1"));
        Assert.assertEquals("val2@" + winnerName, baseMap.get("var2"));
        Assert.assertEquals("val3@" + winnerName, baseMap.get("var3"));
        goalGate.countDown();
        finishedAllTask.await();
        
        Assert.assertEquals(2, taskFinishedThreads.size());
        String looserName = taskFinishedThreads.get(1).getName();
        Assert.assertEquals("val1@" + looserName, baseMap.get("var1"));
        Assert.assertEquals("val2@" + looserName, baseMap.get("var2"));
        Assert.assertEquals("val3@" + looserName, baseMap.get("var3"));
    }

    @Test
    public void testMapMethod() throws Exception {
        final Map<String, String> baseMap = new HashMap<String, String>(){{
            put("var1", "val1");
            put("var2", "val2");
            put("var3", "val3");
        }};
        ExclusiveAccessMap<String, String> map = new ExclusiveAccessMap<String, String>(baseMap, new ReentrantLock());
        assertThat(map.get("var1"), is("val1"));

        assertThat(map.containsKey("var1"), is(true));
        assertThat(map.containsKey("var11"), is(false));

        assertThat(map.containsValue("val2"), is(true));
        assertThat(map.containsValue("val22"), is(false));

        Set<Map.Entry<String,String>> entries = map.entrySet();
        assertThat(entries.size(), is(3));

        assertThat(map.size(), is(3));
        map.putAll(new HashMap<String, String>() {{
            put("a", "b");
        }});
        assertThat(map.size(), is(4));

        assertThat(map.keySet().size(), is(4));
        assertThat(map.values().size(), is(4));

        assertThat(map.remove("a"), is("b"));

        assertThat(map.isEmpty(), is(false));
        map.clear();
        assertThat(map.isEmpty(), is(true));

        map.deactivate();
        map.lock();
        map.lock();
    }
}
