package jiaoni.songfan.service.appengine.guice;

import com.braintreegateway.BraintreeGateway;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.cloud.storage.Storage;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.email.GaeEmailSender;
import jiaoni.common.appengine.access.gcp.GoogleClientFactory;
import jiaoni.common.appengine.access.sms.ConsoleSmsClient;
import jiaoni.common.appengine.access.sms.SmsClient;
import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.guice.ENV;
import jiaoni.songfan.service.appengine.AppEnvs;

import javax.inject.Singleton;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(ENV.class).to(AppEnvs.getEnv());

        bind(BraintreeGateway.class).toProvider(BraintreeGatewayFactory.class).in(Singleton.class);
        bind(StorageClient.class).to(GcsClient.class).in(Singleton.class);
        bind(EmailClient.class).to(GaeEmailSender.class).in(Singleton.class);
        bind(SmsClient.class).to(ConsoleSmsClient.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    DatastoreService provideDatastoreService() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    @Provides
    @Singleton
    AppIdentityService provideAppIdentityService() {
        return AppIdentityServiceFactory.getAppIdentityService();
    }

    @Provides
    @Singleton
    MemcacheService provideMemcacheService() {
        return MemcacheServiceFactory.getMemcacheService();
    }

    @Provides
    @Singleton
    Storage provideStorage() {
        return GoogleClientFactory.storage();
    }
}
