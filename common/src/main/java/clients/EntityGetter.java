package clients;

import com.user_service.model.UserPojo;
import configuration.Configuration;

import java.util.List;

public class EntityGetter {
    private Configuration configuration;
    private UserServiceClient userServiceClient;

    public EntityGetter(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userServiceClient = userServiceClient;
    }

    public List<UserPojo> allUsers() {
        return this.userServiceClient.getAllUsers();
    }
}
