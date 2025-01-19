package configuration;

import clients.*;

public class Toolkit {
    private static Toolkit toolkit = null;
    private Configuration config;
    private UserServiceClient userServiceClient;
    public EntityGetter entityGetter;
    public EntityCreator entityCreator;
    public EntityDelete entityDelete;
    public EntityUpdate entityUpdate;


    public Toolkit() {
        this.config = new Configuration();
        this.userServiceClient = new UserServiceClient(config);
        this.entityGetter = new EntityGetter(config, userServiceClient);
        this.entityCreator = new EntityCreator(config, userServiceClient);
        this.entityDelete = new EntityDelete(config, userServiceClient);
        this.entityUpdate = new EntityUpdate(config, userServiceClient);
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

    public EntityCreator create() {
        return entityCreator;
    }

    public EntityDelete delete() {
        return entityDelete;
    }
    public EntityUpdate update(){
        return entityUpdate;
    }
}
