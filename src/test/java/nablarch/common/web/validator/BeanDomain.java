package nablarch.common.web.validator;

import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.SystemChar;

/**
 * テスト用ドメイン定義.
 */
public class BeanDomain {

    @Length(min = 5, max = 10)
    @SystemChar(charsetDef = "数字")
    public String demoDomain;

}
