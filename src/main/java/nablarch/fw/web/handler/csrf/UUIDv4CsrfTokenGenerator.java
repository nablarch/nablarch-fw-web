package nablarch.fw.web.handler.csrf;

import java.util.UUID;

/**
 * バージョン4のUUIDを使用してCSRFトークンの生成を行うクラス。
 * 
 * @author Uragami Taichi
 *
 */
public class UUIDv4CsrfTokenGenerator implements CsrfTokenGenerator {

    @Override
    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}
