package nablarch.fw.web.download.encode;

import java.util.ArrayList;

import junit.framework.Assert;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderEntry;
import nablarch.fw.web.download.encorder.DownloadFileNameEncoderFactory;
import nablarch.fw.web.download.encorder.MimeBDownloadFileNameEncoder;
import nablarch.fw.web.download.encorder.UrlDownloadFileNameEncoder;
import org.junit.Test;

public class DownloadFileNameEncoderFactoryTest {

    @Test
    public void testGetEncoderUserAgentNull() {
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();
        MimeBDownloadFileNameEncoder encoder = new MimeBDownloadFileNameEncoder();
        factory.setDefaultEncoder(encoder);
        DownloadFileNameEncoder getEncoder = factory.getEncoder(null);
        Assert.assertSame(encoder, getEncoder);
    }
    
    @Test
    public void testGetEncoderUserAgentIE() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder2 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder3 = new UrlDownloadFileNameEncoder();
        MimeBDownloadFileNameEncoder mimeEncoder = new MimeBDownloadFileNameEncoder();

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", urlEncoder1));
        list.add(createEntry(".*Trident.*", urlEncoder3));
        list.add(createEntry(".*WebKit.*", urlEncoder2));
        list.add(createEntry(".*Gecko.*", mimeEncoder));
        
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        factory.setDownloadFileNameEncoderEntries(list);
        
        DownloadFileNameEncoder getEncoder = factory.getEncoder("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        Assert.assertSame(urlEncoder1, getEncoder);
    }

    @Test
    public void testGetEncoderUserAgentIE11() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder2 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder3 = new UrlDownloadFileNameEncoder();
        MimeBDownloadFileNameEncoder mimeEncoder = new MimeBDownloadFileNameEncoder();

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", urlEncoder1));
        list.add(createEntry(".*Trident.*", urlEncoder3));
        list.add(createEntry(".*WebKit.*", urlEncoder2));
        list.add(createEntry(".*Gecko.*", mimeEncoder));
        
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        factory.setDownloadFileNameEncoderEntries(list);
        
        DownloadFileNameEncoder getEncoder = factory.getEncoder("Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko");
        Assert.assertSame(urlEncoder3, getEncoder);
    }

    @Test
    public void testGetEncoderUserAgentWebKit() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder2 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder3 = new UrlDownloadFileNameEncoder();
        MimeBDownloadFileNameEncoder mimeEncoder = new MimeBDownloadFileNameEncoder();

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", urlEncoder1));
        list.add(createEntry(".*Trident.*", urlEncoder3));
        list.add(createEntry(".*WebKit.*", urlEncoder2));
        list.add(createEntry(".*Gecko.*", mimeEncoder));
               
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        factory.setDownloadFileNameEncoderEntries(list);
        
        DownloadFileNameEncoder getEncoder = factory.getEncoder("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.55 Safari/533.4");
        Assert.assertSame(urlEncoder2, getEncoder);
    }
    

    @Test
    public void testGetEncoderUserAgentGecko() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder2 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder3 = new UrlDownloadFileNameEncoder();
        MimeBDownloadFileNameEncoder mimeEncoder = new MimeBDownloadFileNameEncoder();

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", urlEncoder1));
        list.add(createEntry(".*Trident.*", urlEncoder3));
        list.add(createEntry(".*WebKit.*", urlEncoder2));
        list.add(createEntry(".*Gecko.*", mimeEncoder));
               
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        factory.setDownloadFileNameEncoderEntries(list);
        
        DownloadFileNameEncoder getEncoder = factory.getEncoder("  Mozilla/5.0 (Windows; U; Windows NT 5.1; ja; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 GTB7.1 ( .NET CLR 3.5.30729; .NET4.0C)");
        Assert.assertSame(mimeEncoder, getEncoder);
    }

    @Test
    public void testGetEncoderUserAgentNonMatch() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder2 = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder3 = new UrlDownloadFileNameEncoder();
        MimeBDownloadFileNameEncoder mimeEncoder = new MimeBDownloadFileNameEncoder();

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(createEntry(".*MSIE.*", urlEncoder1));
        list.add(createEntry(".*Trident.*", urlEncoder3));
        list.add(createEntry(".*WebKit.*", urlEncoder2));
        list.add(createEntry(".*Gecko.*", mimeEncoder));

        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        factory.setDownloadFileNameEncoderEntries(list);
        
        DownloadFileNameEncoder getEncoder = factory.getEncoder(" マッチしないUser-Agent ");
        Assert.assertSame(defaultEncoder, getEncoder);
    }
    

    @Test
    public void testGetEncoderNotSetUserAgent() {
        UrlDownloadFileNameEncoder defaultEncoder = new UrlDownloadFileNameEncoder();
        UrlDownloadFileNameEncoder urlEncoder1 = new UrlDownloadFileNameEncoder();

        DownloadFileNameEncoderEntry entry = new DownloadFileNameEncoderEntry();
        entry.setEncoder(urlEncoder1);

        ArrayList<DownloadFileNameEncoderEntry> list = new ArrayList<DownloadFileNameEncoderEntry>();
        list.add(entry);
               
        DownloadFileNameEncoderFactory factory = new DownloadFileNameEncoderFactory();;
        factory.setDefaultEncoder(defaultEncoder);
        
        try{
            factory.setDownloadFileNameEncoderEntries(list);
        } catch(RuntimeException e){
            Assert.assertTrue(true);
        }
    }
    
    private DownloadFileNameEncoderEntry createEntry(String userAgentPattern, DownloadFileNameEncoder encoder) {
        DownloadFileNameEncoderEntry entry = new DownloadFileNameEncoderEntry();
        entry.setUserAgentPattern(userAgentPattern);
        entry.setEncoder(encoder);
        return entry;
    }
    
}
