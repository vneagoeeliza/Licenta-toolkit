package configuration;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllureRestTemplate implements ClientHttpRequestInterceptor {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    private static final MediaType JSON_SUBTYPE = MediaType.valueOf("application/*+json");
    private static final MediaType XML_SUBTYPE = MediaType.valueOf("application/*+xml");
    private static final MediaType HTML_SUBTYPE = MediaType.valueOf("application/*+html");

    private static final List<MediaType> LOGGABLE_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.TEXT_HTML,
            JSON_SUBTYPE,
            XML_SUBTYPE,
            HTML_SUBTYPE
    );

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(@NonNull final HttpRequest request, final byte[] body,
                                        @NonNull final ClientHttpRequestExecution execution) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", request.getURI().toString())
                .setMethod(request.getMethodValue())
                .setHeaders(toMapConverter(request.getHeaders()));
        if (body.length != 0 && isLoggableBody(request.getHeaders())) {
            requestAttachmentBuilder.setBody(new String(body, StandardCharsets.UTF_8));
        }

        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(requestAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));

        final ClientHttpResponse clientHttpResponse = execution.execute(request, body);

        final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(clientHttpResponse.getRawStatusCode())
                .setHeaders(toMapConverter(clientHttpResponse.getHeaders()))
                .setBody(this.getBody(clientHttpResponse))
                .build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));

        return clientHttpResponse;
    }

    private String getBody(ClientHttpResponse clientHttpResponse) {
        if (isLoggableBody(clientHttpResponse.getHeaders())) {
            try {
                return StreamUtils.copyToString(clientHttpResponse.getBody(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "Failed to log response body.";
            }
        } else {
            return "Not loggable response body.";
        }
    }

    protected boolean isLoggableBody(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType != null) {
            return LOGGABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith);
        }
        return false;
    }

    private static Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, String.join("; ", value)));
        return result;
    }
}