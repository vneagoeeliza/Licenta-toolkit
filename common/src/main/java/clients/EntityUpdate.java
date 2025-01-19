package clients;

import builders.UserUpdateBuilder;
import configuration.Configuration;

public class EntityUpdate {
    private Configuration configuration;
    private UserUpdateBuilder userUpdateBuilder;

    public EntityUpdate(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userUpdateBuilder = new UserUpdateBuilder(configuration, userServiceClient);

    }
    public UserUpdateBuilder user() {
        return this.userUpdateBuilder;
    }
}

