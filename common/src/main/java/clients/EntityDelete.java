package clients;

import builders.UserCreateBuilder;
import com.user_service.model.UserPojo;
import configuration.Configuration;

import java.util.List;

public class EntityDelete {
    private Configuration configuration;
    private UserServiceClient userServiceClient;

    public EntityDelete(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userServiceClient = userServiceClient;
    }

    public void user(String userId) {
        this.userServiceClient.deleteUser(userId);
    }
}
