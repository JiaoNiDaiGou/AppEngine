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
import jiaoni.common.appengine.access.email.EmailClient;
import jiaoni.common.appengine.access.email.GaeEmailSender;
import jiaoni.common.appengine.access.gcp.GoogleCloudLibFactory;
import jiaoni.common.appengine.access.ocr.GoogleVisionOcrClient;
import jiaoni.common.appengine.access.ocr.OcrClient;
import jiaoni.common.appengine.access.sms.ConsoleSmsClient;
import jiaoni.common.appengine.access.sms.SmsClient;
import jiaoni.common.appengine.access.storage.GcsClient;
import jiaoni.common.appengine.access.storage.StorageClient;
import jiaoni.common.appengine.access.taskqueue.PubSubClient;
import jiaoni.common.appengine.access.taskqueue.TaskQueueClient;
import jiaoni.common.httpclient.InMemoryCookieStore;
import jiaoni.common.httpclient.MockBrowserClient;
import jiaoni.daigou.contentparser.CnAddressParser;
import jiaoni.daigou.contentparser.CnCellPhoneParser;
import jiaoni.daigou.contentparser.CnCustomerContactParser;
import jiaoni.daigou.contentparser.CnPeopleNameParser;
import jiaoni.daigou.lib.teddy.TeddyAdmins;
import jiaoni.daigou.lib.teddy.TeddyClient;
import jiaoni.daigou.lib.teddy.TeddyClientImpl;
import jiaonidaigou.appengine.api.utils.AppEnvironments;

import java.io.IOException;
import javax.inject.Named;
import javax.inject.Singleton;

import static jiaoni.common.model.Env.PROD;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageClient.class).to(GcsClient.class);
        bind(EmailClient.class).to(GaeEmailSender.class);
        bind(PubSubClient.class).to(TaskQueueClient.class);
        bind(OcrClient.class).to(GoogleVisionOcrClient.class);
        bind(SmsClient.class).to(ConsoleSmsClient.class);

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
}
