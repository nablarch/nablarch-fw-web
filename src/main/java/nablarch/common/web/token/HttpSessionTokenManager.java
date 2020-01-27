package nablarch.common.web.token;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

import javax.servlet.http.HttpSession;

/**
 * HttpSessionを使った{@link TokenManager}実装クラス
 */
public class HttpSessionTokenManager implements TokenManager {

    @Override
    public void saveToken(String serverToken, NablarchHttpServletRequestWrapper request) {
        HttpSession session = request.getSession(true).getDelegate();
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        synchronized (session) {
            session.setAttribute(webConfig.getDoubleSubmissionTokenSessionAttributeName(),
                    serverToken);
        }
    }

    @Override
    public boolean isValidToken(String clientToken, ServletExecutionContext context) {
        HttpSession session = context.getNativeHttpSession(false);
        if (session == null) {
            return false;
        }
        String name = WebConfigFinder.getWebConfig().getDoubleSubmissionTokenSessionAttributeName();
        String serverToken = (String) session.getAttribute(name);
        session.removeAttribute(name);
        return clientToken.equals(serverToken);

    }
}
