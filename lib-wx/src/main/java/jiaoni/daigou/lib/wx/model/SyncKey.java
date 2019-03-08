package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SyncKey {
    // Count of Key pair list.
    @JsonProperty("Count")
    private int count;

    @JsonProperty("List")
    private List<KeyPair> list;

    public static class KeyPair {
        @JsonProperty("Key")
        private int key;

        @JsonProperty("Val")
        private long val;

        public int getKey() {
            return key;
        }

        public long getVal() {
            return val;
        }
    }

    public int getCount() {
        return count;
    }

    public List<KeyPair> getList() {
        return list;
    }
}
