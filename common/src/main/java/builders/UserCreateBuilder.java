package builders;

import clients.EntityCreateBuilder;
import clients.UserServiceClient;
import com.user_service.model.UserPojo;
import configuration.Configuration;
import org.springframework.http.ResponseEntity;

public class UserCreateBuilder implements EntityCreateBuilder<UserPojo> {
    private Configuration configuration;
    private UserServiceClient userServiceClient;
    private UserPojo user = new UserPojo();

    public UserCreateBuilder(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userServiceClient = userServiceClient;
    }

    public UserCreateBuilder withName(String name) {
        user.setName(name);
        return this;
    }

    public UserCreateBuilder withGender(String gender) {
        user.setGender(gender);
        return this;
    }

    public UserCreateBuilder withEmail(String email) {
        user.setEmail(email);
        return this;
    }

    public UserCreateBuilder withStatus(String status) {
        user.setStatus(status);
        return this;
    }

    @Override
    public ResponseEntity<UserPojo> execute() {
        return userServiceClient.createUser(user);
    }
}
