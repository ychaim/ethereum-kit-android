package io.horizontalsystems.ethereumkit.light.net.message;

public abstract class LesMessage extends Message {

    public LesMessage() {
    }

    public LesMessage(byte[] encoded) {
        super(encoded);
    }

    abstract public LesMessageCodes getCommand();
}
