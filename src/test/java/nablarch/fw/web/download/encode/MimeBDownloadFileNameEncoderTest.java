package nablarch.fw.web.download.encode;

import java.io.UnsupportedEncodingException;

import nablarch.fw.web.download.encorder.MimeBDownloadFileNameEncoder;
import org.junit.Assert;
import org.junit.Test;

public class MimeBDownloadFileNameEncoderTest {

    @Test
    public void testDefaultEncode() {
        MimeBDownloadFileNameEncoder encoder = new MimeBDownloadFileNameEncoder();
        String encode = encoder.encode("かきくけこ");
        Assert.assertEquals("=?UTF-8?B?44GL44GN44GP44GR44GT?=", encode);
    }

    @Test
    public void testEncodeShift_JIS() throws UnsupportedEncodingException {
        MimeBDownloadFileNameEncoder encoder = new MimeBDownloadFileNameEncoder();
        encoder.setCharset("Shift_JIS");
        String encode = encoder.encode("かきくけこ");
        Assert.assertEquals("=?Shift_JIS?B?gqmCq4Ktgq+CsQ==?=", encode);
    }    

    
    @Test
    public void testEncodeCharsetIllegal() {
        MimeBDownloadFileNameEncoder encoder = new MimeBDownloadFileNameEncoder();
        encoder.setCharset("illegal");
        try{
            encoder.encode("かきくけこ");
            Assert.fail();
        } catch(RuntimeException e) {
            Assert.assertTrue(true);
        }
    }
    
    

    
}
