package jiaoni.daigou.lib.wx;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;

public class WxReply {
    private List<ReplyAction> actions;

    private WxReply(Builder builder) {
        this.actions = builder.actions;
    }

    public static WxReply empty() {
        return WxReply.builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ReplyAction> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("actions", actions)
                .toString();
    }

    public interface ReplyAction {
        String getToUserName();
    }

    public static class TextReply implements ReplyAction {
        private String text;
        private String toUserName;

        private TextReply(final String toUserName,
                          final String text) {
            this.toUserName = toUserName;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String getToUserName() {
            return toUserName;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("text", text)
                    .add("toUserName", toUserName)
                    .toString();
        }
    }

//    public static class ImageReply implements ReplyAction {
//        private List<Image> images;
//        private String toUserName;
//
//        private ImageReply(final String toUserName, final List<Image> images) {
//            this.toUserName = toUserName;
//            this.images = images;
//        }
//
//        public List<Image> getImages() {
//            return images;
//        }
//
//        @Override
//        public String getToUserName() {
//            return toUserName;
//        }
//    }

    public static final class Builder {
        private List<ReplyAction> actions = new ArrayList<>();

        private Builder() {
        }

        public Builder text(final String toUserName, final String text) {
            actions.add(new TextReply(toUserName, text));
            return this;
        }

//        public Builder image(final String toUserName, final Image image) {
//            return images(toUserName, Collections.singletonList(image));
//        }
//
//        public Builder images(final String toUserName, final List<Image> images) {
//            actions.add(new ImageReply(toUserName, images));
//            return this;
//        }

        public WxReply build() {
            return new WxReply(this);
        }
    }
}
