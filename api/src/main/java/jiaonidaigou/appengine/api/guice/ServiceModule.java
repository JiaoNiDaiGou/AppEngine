package jiaonidaigou.appengine.api.guice;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jiaonidaigou.appengine.api.access.db.CustomerDbClient;
import jiaonidaigou.appengine.api.access.db.ProductDbClient;
import jiaonidaigou.appengine.api.access.email.EmailClient;
import jiaonidaigou.appengine.api.access.email.GaeEmailSender;
import jiaonidaigou.appengine.api.access.gcp.GoogleCloudLibFactory;
import jiaonidaigou.appengine.api.access.ocr.GoogleVisionOcrClient;
import jiaonidaigou.appengine.api.access.ocr.OcrClient;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.api.access.taskqueue.PubSubClient;
import jiaonidaigou.appengine.api.access.taskqueue.TaskQueueClient;
import jiaonidaigou.appengine.api.utils.AppEnvironments;
import jiaonidaigou.appengine.common.httpclient.InMemoryCookieStore;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.contentparser.CnAddressParser;
import jiaonidaigou.appengine.contentparser.CnCellPhoneParser;
import jiaonidaigou.appengine.contentparser.CnCustomerContactParser;
import jiaonidaigou.appengine.contentparser.CnPeopleNameParser;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;

import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaonidaigou.appengine.common.utils.Environments.NAMESPACE_JIAONIDAIGOU;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageClient.class).to(GcsClient.class);
        bind(EmailClient.class).to(GaeEmailSender.class);
        bind(PubSubClient.class).to(TaskQueueClient.class);
        bind(OcrClient.class).to(GoogleVisionOcrClient.class);

        bind(CnCustomerContactParser.class).toInstance(new CnCustomerContactParser());
        bind(CnAddressParser.class).toInstance(new CnAddressParser());
        bind(CnPeopleNameParser.class).toInstance(new CnPeopleNameParser());
        bind(CnCellPhoneParser.class).toInstance(new CnCellPhoneParser());
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
    @Named(TeddyAdmins.JIAONI)
    TeddyClient provideTeddyClientJiaoni() {
        return new TeddyClientImpl(TeddyAdmins.JIAONI,
                new MockBrowserClient("teddyclient." + TeddyAdmins.JIAONI, new InMemoryCookieStore()));
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.HACK)
    TeddyClient provideTeddyClientHack() {
        return new TeddyClientImpl(TeddyAdmins.HACK,
                new MockBrowserClient("teddyclient." + TeddyAdmins.HACK, new InMemoryCookieStore()));
    }

    @Provides
    @Singleton
    @Named(TeddyAdmins.BY_ENV)
    TeddyClient provideTeddyClientByEnv() {
        switch (AppEnvironments.ENV) {
            case PROD:
                return new TeddyClientImpl(TeddyAdmins.JIAONI,
                        new MockBrowserClient("teddyclient." + TeddyAdmins.JIAONI, new InMemoryCookieStore()));
            default:
                return new TeddyClientImpl(TeddyAdmins.HACK,
                        new MockBrowserClient("teddyclient." + TeddyAdmins.HACK, new InMemoryCookieStore()));
        }
    }

    @Provides
    @Singleton
    ImageAnnotatorClient provideImageAnnotatorClient() {
        try {
            return ImageAnnotatorClient.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    Storage provideStorage() {
        return GoogleCloudLibFactory.storage();
    }

    @Provides
    @Singleton
    @JiaoNiDaiGou
    CustomerDbClient provideJiaoNiDaiGouCustomerDbClient(final DatastoreService datastoreService) {
        return new CustomerDbClient(datastoreService, NAMESPACE_JIAONIDAIGOU);
    }

    @Provides
    @Singleton
    @JiaoNiDaiGou
    ProductDbClient provideJiaoNiDaiGouProductDbClient(final DatastoreService datastoreService) {
        return new ProductDbClient(datastoreService, NAMESPACE_JIAONIDAIGOU);
    }
}
