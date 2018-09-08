package jiaonidaigou.appengine.api;

import com.google.inject.AbstractModule;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageClient.class).to(GcsClient.class);
    }
}
