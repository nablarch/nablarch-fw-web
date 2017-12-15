package nablarch.common.web.validator;

import nablarch.core.message.Message;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final Serializable object;
    private final List<Message> messages;

    public static ValidationResult createValidResult(Serializable object) {
        return new ValidationResult(object, Collections.<Message>emptyList());
    }

    public static ValidationResult createInvaidResult(List<Message> messages) {
        return new ValidationResult(null, messages);
    }

    private ValidationResult(Serializable object, List<Message> messages) {
        this.object = object;
        this.messages = messages;
    }

    public boolean isValid() {
        return messages.isEmpty();
    }

    public List<Message> getMessage() {
        return messages;
    }

    public Serializable getObject() {
        return object;
    }
}
