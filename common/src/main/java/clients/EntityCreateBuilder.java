package clients;

public interface EntityCreateBuilder<T> {
    T execute() throws InterruptedException;

    @SuppressWarnings("unchecked")
    default <V extends Builder<?>, U extends EntityCreateBuilder<T>> U withOverride(final OverrideStatement<V> override) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
