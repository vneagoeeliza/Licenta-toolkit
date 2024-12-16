package clients;

import com.user_service.api.UserServiceApi;
import com.user_service.model.UserPojo;
import configuration.Configuration;

import java.util.List;

public class UserServiceClient {
    private UserServiceApi userServiceApi;
    private Configuration configuration;

    public UserServiceClient(Configuration config) {
        this.configuration = config;
        this.userServiceApi = new UserServiceApi(config.getApiClient());
    }

    public List<UserPojo> getAllUsers() {
        return userServiceApi.searchAllUsers();
    }

    public UserPojo createUser(UserPojo userPojo) {
        return userServiceApi.createUser(userPojo);
    }

    public void deleteUser(String userId) {
        userServiceApi.deleteUser(userId);
    }
}
