package io.pacworx.atp.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class FcmCommand extends HystrixCommand<Void> {
    private static final Logger log = LogManager.getLogger();

    private String fcmServerKey;
    private String body;

    public FcmCommand(String fcmServerKey, String json) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("fcm command")));
        this.fcmServerKey = fcmServerKey;
        this.body = json;
    }

    @Override
    protected Void run() throws Exception {
        try {
            HttpPost post = createPost();
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = httpclient.execute(post);

            if (response.getStatusLine().getStatusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                FcmResponse fcmResponse = mapper.readValue(response.getEntity().getContent(), FcmResponse.class);
                log.info("Send push notification. success: " + fcmResponse.getSuccess() + ", failure: " + fcmResponse.getFailure());
            } else {
                log.info("Notification body that lead to an error: " + this.body);
                log.error("Notification response (" + response.getStatusLine().getStatusCode() + "): " + EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private HttpPost createPost() throws Exception {
        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost("fcm.googleapis.com")
                .setPath("/fcm/send")
                .build();
        HttpPost post = new HttpPost(uri);
        post.setHeader("Authorization", "key=" + fcmServerKey);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(body));
        return post;
    }
}
