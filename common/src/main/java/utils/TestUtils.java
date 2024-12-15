package utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class TestUtils {
    private HttpHeaders headers = new HttpHeaders();
    private RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        return "43225c35777b5c2458c5566386c5c876c43db255ef04eb022d76ee284afd113c";
    }
}
