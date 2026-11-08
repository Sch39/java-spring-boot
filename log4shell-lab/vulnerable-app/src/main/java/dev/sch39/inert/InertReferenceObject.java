package dev.sch39.inert;

/**
 * Objek penanda yang dihasilkan oleh InertReferenceObjectFactory.
 * Tidak memiliki perilaku, hanya menyimpan metadata hasil resolusi.
 */
public final class InertReferenceObject {

    private final String type;
    private final String source;
    private final boolean executed;

    public InertReferenceObject(String type, String source, boolean executed) {
        this.type = type;
        this.source = source;
        this.executed = executed;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public boolean isExecuted() {
        return executed;
    }

    @Override
    public String toString() {
        return "InertReferenceObject{" +
                "type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", executed=" + executed +
                '}';
    }
}