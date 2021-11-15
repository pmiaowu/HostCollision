package Bootstrap;

import com.github.kevinsawicki.http.HttpRequest;

import java.net.URL;

public class HttpCustomRequest {
    private HttpRequest request;
    private String host;
    private String body;

    public HttpCustomRequest(HttpRequest request, String host) {
        String body = request.body();
        this.request = request;
        this.host = host;
        this.body = body;
    }

    public String host() {
        return this.host;
    }

    public String body() {
        return this.body;
    }

    public HttpRequest request() {
        return request;
    }

    public Integer contentLength() {
        return request().contentLength();
    }

    public String location() {
        return request().location();
    }

    public String title() {
        return CustomHelpers.getBodyTitle(appBody());
    }

    public String appBody() {
        String body;
        String requestLocation = request.location();

        if (requestLocation != null) {
            try {
                // 如果遇到了 Location 提取出来url即可
                // 例如:
                //  Location: http://domainwall.cloud.baidu.com/block.html
                //  提取为: http://domainwall.cloud.baidu.com
                URL url = new URL(requestLocation);
                body = url.getProtocol() + "://" + url.getHost();
            } catch (Exception e) {
                // 到这一步说明, Location 没有带url
                // 例如:
                // Location: /?WebShieldDRSessionVerify=2fwUQSqDgGK3uZw6eIVS
                // 这种对程序来说其实是垃圾数据,所以直接把body设置为空是最好的
                body = "";
            }
        } else {
            body = body();
        }

        return body;
    }

    public String bodyFormat() {
        return appBody().replace(host, "");
    }

    public String filteredPageContent() {
        return bodyFormat();
    }

    public String header(String name) {
        return request().header(name);
    }

    public String server() {
        return request().server();
    }

    public String XPoweredBy() {
        return request().header("X-Powered-By");
    }

    public Integer code() {
        return request().code();
    }
}
