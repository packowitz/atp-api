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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class FcmCommand extends HystrixCommand<Void> {

    private static final Logger LOGGER = LogManager.getLogger(FcmCommand.class);

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
            LOGGER.info("Posting notification: " + this.body);
            HttpPost post = createPost();
            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = httpclient.execute(post);
            LOGGER.info("Notification response: " + response.getEntity().toString());

            ObjectMapper mapper = new ObjectMapper();
            FcmResponse fcmResponse = mapper.readValue(response.getEntity().getContent(), FcmResponse.class);
            LOGGER.info("Send push notification. success: " + fcmResponse.getSuccess() + ", failure: " + fcmResponse.getFailure());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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
