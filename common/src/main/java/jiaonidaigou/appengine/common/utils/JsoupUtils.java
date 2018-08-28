package jiaonidaigou.appengine.common.utils;

import com.google.common.base.Preconditions;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class JsoupUtils {
    @Nullable
    public static String getElementTextById(final Element root, final String id) {
        Element ele = root.getElementById(id);
        return ele == null ? null : ele.text();
    }

    @Nullable
    public static String getChildText(final Element root, final int... childIdx) {
        checkArgument(childIdx.length > 0);
        Element ele = root;
        for (int i : childIdx) {
            ele = ele.child(i);
            if (ele == null) {
                break;
            }
        }
        return ele == null ? null : ele.text();
    }
}
