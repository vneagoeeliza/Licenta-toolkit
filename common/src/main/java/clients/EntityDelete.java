package clients;

import configuration.Configuration;
import org.springframework.http.ResponseEntity;

public class EntityDelete {
    private Configuration configuration;
    private UserServiceClient userServiceClient;

    public EntityDelete(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userServiceClient = userServiceClient;
    }

    public ResponseEntity<Void> user(String userId) {
        return this.userServiceClient.deleteUser(userId);
    }

}
