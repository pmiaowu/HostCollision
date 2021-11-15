package Bootstrap;

public class ReturnFormat {
    private Boolean result;
    private HttpCustomRequest request;

    public ReturnFormat(Boolean result, HttpCustomRequest request) {
        this.result = result;
        this.request = request;
    }

    public Boolean result() {
        return this.result;
    }

    public HttpCustomRequest request() {
        return this.request;
    }
}
