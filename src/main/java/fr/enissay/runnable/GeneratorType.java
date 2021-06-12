package fr.enissay.runnable;

public enum GeneratorType {

    CLASSIC(16),
    BOOST(24);

    private int length;

    GeneratorType(final int length){
        this.length = length;
    }

    public int getLength() {
        return length;
    }
}
