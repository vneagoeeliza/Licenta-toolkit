package configuration;

import com.user_service.api.ApiClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import utils.TestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class Configuration {
    private String userServiceUrl;
    private String accessToken;
    private TestUtils testUtils = new TestUtils();

    public Configuration() {
        setServiceUrl();
        getAccessToken();
    }
    private void setServiceUrl() {
        userServiceUrl= "https://gorest.co.in/public/v2";
    }
    private void getAccessToken() {
        accessToken = "Bearer " + testUtils.getAccessToken();
    }

    private ClientHttpResponse getAccessTokenForInterceptor(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        if (accessToken == null) {
            accessToken = "Bearer " + testUtils.getAccessToken();
                    }
        HttpRequest authRequest = new HttpRequestWrapper(httpRequest);
        HttpHeaders headers = httpRequest.getHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, accessToken);
        return clientHttpRequestExecution.execute(authRequest, bytes);
    }
    private ClientHttpResponse addRequestId(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        HttpRequest authRequest = new HttpRequestWrapper(httpRequest);
        HttpHeaders headers = httpRequest.getHeaders();
        headers.add("Request-id", "Req-Id" + UUID.randomUUID());
        return clientHttpRequestExecution.execute(authRequest, bytes);
    }

    private RestTemplate createAuthorizedTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        AllureRestTemplate allureRestTemplate = new AllureRestTemplate();

        RestTemplateLoggingInterceptor restTemplateLoggingInterceptor = new RestTemplateLoggingInterceptor();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        restTemplate.setRequestFactory(requestFactory);
        ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(allureRestTemplate);
        interceptors.add(this::getAccessTokenForInterceptor);
        interceptors.add(this::addRequestId);
        interceptors.add(restTemplateLoggingInterceptor);

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
    public ApiClient getVoConfigApiClient() {
        ApiClient apiClient = new ApiClient(createAuthorizedTemplate());
        apiClient.setBasePath(userServiceUrl);
        return apiClient;
    }
}
