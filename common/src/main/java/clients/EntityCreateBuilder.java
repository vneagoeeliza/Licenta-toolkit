package clients;

import com.user_service.model.UserPojo;
import org.springframework.http.ResponseEntity;

public interface EntityCreateBuilder<T> {
    ResponseEntity<UserPojo> execute() throws InterruptedException;

    @SuppressWarnings("unchecked")
    default <V extends Builder<?>, U extends EntityCreateBuilder<T>> U withOverride(final OverrideStatement<V> override) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
