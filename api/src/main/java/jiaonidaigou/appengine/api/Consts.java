package jiaonidaigou.appengine.api;

import jiaonidaigou.appengine.common.utils.Environments;

import javax.ws.rs.core.MediaType;

public interface Consts {
    String GCS_MEDIA_ROOT_ENDSLASH = Environments.GCS_ROOT_ENDSLASH + "media/";
    MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_OCTET_STREAM_TYPE;
}
