package nablarch.fw.web.handler.normalizer;

/**
 * 前後のホワイトスペース({@link Character#isWhitespace(int)})を除去するノーマライザ実装クラス。
 *
 * @author Hisaaki Shioiri
 */
public class TrimNormalizer implements Normalizer {

    /**
     * 全てのパラメータがトリム対象なので、常に{@code true}を返す。
     */
    @Override
    public boolean canNormalize(final String key) {
        return true;
    }

    /**
     * 前後のホワイトスペースを除去した値を返す。
     *
     * @return 前後のホワイトスペースを除去した値
     */
    @Override
    public String[] normalize(final String[] value) {
        final String[] result = new String[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = trimWhiteSpace(value[i]);
        }
        return result;
    }

    /**
     * ホワイトスペースをトリムする。
     *
     * @param value トリム対象の値
     * @return トリム後の値
     */
    private static String trimWhiteSpace(final String value) {
        int start = value.length();
        int end = value.length();
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.codePointAt(i))) {
                start = i;
                break;
            }
        }

        for (int i = value.length(); i > 0; i--) {
            if (!Character.isWhitespace(value.charAt(i - 1))) {
                end = i;
                break;
            }
        }
        return value.substring(start, end);
    }
}
