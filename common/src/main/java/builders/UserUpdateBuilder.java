package builders;

import clients.EntityCreateBuilder;
import clients.UserServiceClient;
import com.user_service.model.UserPojo;
import configuration.Configuration;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

public class UserUpdateBuilder implements EntityCreateBuilder<UserPojo> {
    private Configuration configuration;
    private UserServiceClient userServiceClient;
    private UserPojo user = new UserPojo();
    @Getter
    private String userId;

    public UserUpdateBuilder(Configuration configuration, UserServiceClient userServiceClient) {
        this.configuration = configuration;
        this.userServiceClient = userServiceClient;
    }
    public UserUpdateBuilder withId(String userId) {
        this.userId = userId;
        user.setId(userId);
        return this;
    }
    public UserUpdateBuilder withName(String name) {
        user.setName(name);
        return this;
    }

    public UserUpdateBuilder withGender(String gender) {
        user.setGender(gender);
        return this;
    }

    public UserUpdateBuilder withEmail(String email) {
        user.setEmail(email);
        return this;
    }

    public UserUpdateBuilder withStatus(String status) {
        user.setStatus(status);
        return this;
    }

    @Override
    public ResponseEntity<UserPojo> execute() {
        return userServiceClient.updateUser(getUserId(), user);
    }
}
