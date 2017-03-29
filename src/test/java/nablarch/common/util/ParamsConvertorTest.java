package nablarch.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.common.web.WebTestUtil;
import nablarch.common.web.hiddenencryption.TamperingDetectedException;
import org.junit.Test;

/**
 * @author Kiyohito Itoh
 */
public class ParamsConvertorTest {
    
    @SuppressWarnings("serial")
    @Test
    public void testConvert() {
        
        ParamsConvertor convertor = new ParamsConvertor(',', '=', '\\');
        
        Map<String, List<String>> params = new HashMap<String, List<String>>() {
            {
                // normal
                put("aaa", new ArrayList<String>() {{add("bbb"); add("ccc"); add("ddd"); add("eee"); add("fff");}});
                // blank
                put("", new ArrayList<String>() {{add(""); add(" "); add("  "); add("   "); add("    ");}});
                // param separator
                put(",", new ArrayList<String>() {{add(",,"); add(",,,"); add(",,,,"); add(",,,,,");}});
                // name/value separator
                put("=", new ArrayList<String>() {{add("=="); add("==="); add("===="); add("=====");}});
                // escape char
                put("\\", new ArrayList<String>() {{add("\\\\"); add("\\\\\\"); add("\\\\\\\\"); add("\\\\\\\\\\");}});
                // escaped char
                put("\\,\\=\\\\", new ArrayList<String>() {{add("\\=\\\\\\,"); add("\\\\\\,\\="); add("\\\\\\=\\,");}});
                // line separator
                put("\\,=\naaa", new ArrayList<String>() {{add("\nbbb"); add("ccc\n"); add("ddd\neee"); add("\\=,\n,=\\");}});
            }
        };
        
        String paramsStr = convertor.convert(params);
        Map<String, List<String>> actualParams = convertor.convert(paramsStr);
        
        WebTestUtil.assertParams(actualParams, params);
    }
    
    @SuppressWarnings("serial")
    @Test
    public void testConvertForStartWithParamSeparator() {
        
        ParamsConvertor convertor = new ParamsConvertor(',', '=', '\\');
        
        Map<String, List<String>> params = new HashMap<String, List<String>>() {
            {
                put(",", new ArrayList<String>() {{add("aaa");}});
            }
        };
        
        String paramsStr = convertor.convert(params);
        System.out.println(paramsStr);
        Map<String, List<String>> actualParams = convertor.convert(paramsStr);
        
        WebTestUtil.assertParams(actualParams, params);
    }
    
    @Test
    public void testInvalidEscape() {
        
        ParamsConvertor convertor = new ParamsConvertor(',', '=', '\\');
        
        // invalid escape
        
        String badParams = "aaa=bbb,ccc=d\\dd,eee=fff";
        try {
            convertor.convert(badParams);
            fail("must throw IllegalArgumentException.");
        } catch (TamperingDetectedException e) {
            assertThat(e.getMessage(), is("failed to convert. params = [aaa=bbb,ccc=d\\dd,eee=fff], param = [ccc=d\\dd]"));
            assertThat(e.getCause().getMessage(), is("invalid escape sequence was found. nameValueStr = [ccc=d\\dd]"));
        }
        
        badParams = "aaa=bbb,ccc=ddd,,eee=fff";
        try {
            convertor.convert(badParams);
            fail("must throw IllegalArgumentException.");
        } catch (TamperingDetectedException e) {
            assertThat(e.getMessage(), is("failed to convert. params = [aaa=bbb,ccc=ddd,,eee=fff], param = []"));
            assertThat(e.getCause().getMessage(), is("invalid escape sequence was found. nameValueStr = []"));
        }
        
        // start with param separator
        badParams = ",aaa=bbb,ccc=ddd,eee=fff";
        try {
            convertor.convert(badParams);
            fail("must throw IllegalArgumentException.");
        } catch (TamperingDetectedException e) {
            assertThat(e.getMessage(), is("failed to convert. params = [,aaa=bbb,ccc=ddd,eee=fff], param = []"));
            assertThat(e.getCause().getMessage(), is("invalid escape sequence was found. str = [,aaa=bbb,ccc=ddd,eee=fff]"));
        }
        // end with param separator
        badParams = "aaa=bbb,ccc=ddd,eee=fff,";
        try {
            convertor.convert(badParams);
            fail("must throw IllegalArgumentException.");
        } catch (TamperingDetectedException e) {
            assertThat(e.getMessage(), is("failed to convert. params = [aaa=bbb,ccc=ddd,eee=fff,], param = [eee=fff]"));
            assertThat(e.getCause().getMessage(), is("invalid escape sequence was found. str = [aaa=bbb,ccc=ddd,eee=fff,]"));
        }
    }
}
