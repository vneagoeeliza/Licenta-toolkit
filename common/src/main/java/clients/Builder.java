package clients;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hamcrest.Matcher;

import java.util.HashMap;
import java.util.Map;

public abstract class Builder<T> {
    @JsonIgnore
    private final Map<String, Matcher<?>> fieldMatchers = new HashMap<>();

    public abstract T build();

    protected void setMatcher(final String fieldName, final Matcher<?> matcher) {
        fieldMatchers.put(fieldName, matcher);
    }
}
