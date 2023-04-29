package clients;

import builders.UserCreateBuilder;
import configuration.Configuration;

public class EntityCreator {
    private Configuration configuration;
    private UserCreateBuilder userCreateBuilder;

    public EntityCreator(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userCreateBuilder = new UserCreateBuilder(configuration, userServiceClient);
    }

    public UserCreateBuilder user() {
        return this.userCreateBuilder;
    }
}
