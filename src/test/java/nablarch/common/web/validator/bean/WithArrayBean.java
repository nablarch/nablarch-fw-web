package nablarch.common.web.validator.bean;

import java.io.Serializable;

public class WithArrayBean implements Serializable {

    private String id;

    private String[] numbers;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String[] getNumbers() {
        return numbers;
    }

    public void setNumbers(final String[] numbers) {
        this.numbers = numbers;
    }
}
