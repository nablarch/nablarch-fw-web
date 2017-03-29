package nablarch.fw.web.download.encode;

import nablarch.fw.web.download.encorder.UrlDownloadFileNameEncoder;
import org.junit.Assert;
import org.junit.Test;

public class UrlDownloadFileNameEncoderTest {

    @Test
    public void testEncodeUserAgent(){
        UrlDownloadFileNameEncoder encoder = new UrlDownloadFileNameEncoder();
        String encode = encoder.encode("かきくけこ");
        Assert.assertEquals("%E3%81%8B%E3%81%8D%E3%81%8F%E3%81%91%E3%81%93", encode);
    }
    
    
    @Test
    public void testEncodeUserAgentShift_JIS() {
        UrlDownloadFileNameEncoder encoder = new UrlDownloadFileNameEncoder();
        encoder.setCharset("Shift_JIS");
        String encode = encoder.encode("かきくけこ");
      	Assert.assertEquals("%82%A9%82%AB%82%AD%82%AF%82%B1", encode);
    }
    
    @Test
    public void testEncodeUserAgentIllegalCharset() {
        UrlDownloadFileNameEncoder encoder = new UrlDownloadFileNameEncoder();
        encoder.setCharset("illegal");
        try{
            encoder.encode("かきくけこ");
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertTrue(true);
        }        
    }
    
}
