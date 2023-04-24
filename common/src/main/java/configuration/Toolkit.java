package configuration;

import clients.EntityGetter;
import clients.UserServiceClient;

public class Toolkit {
    private static Toolkit toolkit = null;
    private Configuration config;
    private UserServiceClient userServiceClient;
    public EntityGetter entityGetter;


    public Toolkit() {
        this.config = new Configuration();
        this.userServiceClient = new UserServiceClient(config);
        this.entityGetter = new EntityGetter(config, userServiceClient);
    }

    public static Toolkit toolkit() {
        if (toolkit == null) {
            toolkit = new Toolkit();
        }
        return toolkit;
    }
    public EntityGetter get() {
        return entityGetter;
    }
}
