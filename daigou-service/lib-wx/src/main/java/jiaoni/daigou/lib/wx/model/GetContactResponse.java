package jiaoni.daigou.lib.wx.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of webwxgetcontact
 */
public class GetContactResponse {
    @JsonProperty("BaseResponse")
    private BaseResponse baseResponse;
    @JsonProperty("MemberCount")
    private int memberCount;
    @JsonProperty("MemberList")
    private List<Contact> memberList;
    @JsonProperty("Seq")
    private int seq;

    public BaseResponse getBaseResponse() {
        return baseResponse;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public List<Contact> getMemberList() {
        return memberList;
    }

    public int getSeq() {
        return seq;
    }
}
