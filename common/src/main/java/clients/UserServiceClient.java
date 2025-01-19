package clients;

import com.user_service.api.UserServiceApi;
import com.user_service.model.UserPojo;
import configuration.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class UserServiceClient {
    private UserServiceApi userServiceApi;
    private Configuration configuration;

    public UserServiceClient(Configuration config) {
        this.configuration = config;
        this.userServiceApi = new UserServiceApi(config.getApiClient());
    }

    public ResponseEntity<List<UserPojo>> getAllUsers() {
        return userServiceApi.searchAllUsersWithHttpInfo();
    }
    public ResponseEntity<UserPojo> getSingleUser(String userId) {
        return userServiceApi.getUserByIdWithHttpInfo(userId);
    }

    public ResponseEntity<UserPojo> createUser(UserPojo userPojo) {
        return userServiceApi.createUserWithHttpInfo(userPojo);
    }

    public ResponseEntity<Void> deleteUser(String userId) {
       return userServiceApi.deleteUserWithHttpInfo(userId);
    }

    public ResponseEntity<UserPojo> updateUser(String userId, UserPojo userPojo){
        return userServiceApi.updateUserWithHttpInfo(userId, userPojo);
    }
}
