package jiaonidaigou.appengine.api.guice;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import jiaonidaigou.appengine.api.access.storage.GcsClient;
import jiaonidaigou.appengine.api.access.storage.StorageClient;
import jiaonidaigou.appengine.common.httpclient.InMemoryCookieStore;
import jiaonidaigou.appengine.common.httpclient.MockBrowserClient;
import jiaonidaigou.appengine.lib.ocrspace.OcrSpaceClient;
import jiaonidaigou.appengine.lib.teddy.TeddyAdmins;
import jiaonidaigou.appengine.lib.teddy.TeddyClient;
import jiaonidaigou.appengine.lib.teddy.TeddyClientImpl;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageClient.class).to(GcsClient.class);
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
    OcrSpaceClient provideOcrSpaceClient() {
        return new OcrSpaceClient(new MockBrowserClient("ocrspaceclient", new InMemoryCookieStore()));
    }
}
