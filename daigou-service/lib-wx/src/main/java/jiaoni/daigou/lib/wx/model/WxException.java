package jiaoni.daigou.lib.wx.model;

public class WxException extends RuntimeException {
    private BaseResponse baseResponse;

    public WxException() {
        super();
    }

    public WxException(String message) {
        super(message);
    }

    public WxException(String message, Throwable cause) {
        super(message, cause);
    }

    public WxException(Throwable cause) {
        super(cause);
    }

    public WxException withWxBaseResponse(final BaseResponse baseResponse) {
        this.baseResponse = baseResponse;
        return this;
    }

    public BaseResponse getBaseResponse() {
        return baseResponse;
    }
}
