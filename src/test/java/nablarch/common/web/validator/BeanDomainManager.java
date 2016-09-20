package nablarch.common.web.validator;

import nablarch.core.validation.ee.DomainManager;

/**
 * テスト用ドメイン定義の管理クラス.
 */
public class BeanDomainManager implements DomainManager<BeanDomain> {

    @Override
    public Class<BeanDomain> getDomainBean() {
        return BeanDomain.class;
    }

}
