package nablarch.common.web.token;

import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

public interface TokenManager {

    void saveToken(String serverToken, NablarchHttpServletRequestWrapper request);

    boolean isValidToken(String clientToken, ServletExecutionContext context);
}
