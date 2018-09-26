package jiaonidaigou.appengine.api.access.ocr;

import jiaonidaigou.appengine.common.model.Snippet;
import jiaonidaigou.appengine.wiremodel.entity.MediaObject;

import java.util.List;

public interface OcrClient {
    List<Snippet> annotateFromBytes(final byte[] bytes);

    List<Snippet> annotateFromMediaObject(final MediaObject mediaObject);
}
