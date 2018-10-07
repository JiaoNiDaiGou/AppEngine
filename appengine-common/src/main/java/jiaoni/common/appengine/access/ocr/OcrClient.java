package jiaoni.common.appengine.access.ocr;

import jiaoni.common.model.Snippet;

import java.util.List;

public interface OcrClient {
    List<Snippet> annotateFromBytes(final byte[] bytes);

    List<Snippet> annotateFromMediaPath(final String mediaPath);
}
