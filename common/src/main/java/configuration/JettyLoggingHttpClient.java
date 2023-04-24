package configuration;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class JettyLoggingHttpClient extends HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(JettyLoggingHttpClient.class);

    public JettyLoggingHttpClient(SslContextFactory sslContextFactory) {
        super(sslContextFactory);
    }

    protected List<String> getObfuscatedHeaders() {
        return List.of("Authorization");
    }

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    @Override
    public Request newRequest(URI uri) {
        Request request = super.newRequest(uri);
        return enhance(request);
    }

    Request enhance(Request request) {
        StringBuilder group = new StringBuilder();

        AtomicReference<String> uri = new AtomicReference<>("no uri");
        AtomicReference<String> method = new AtomicReference<>("no method");
        AtomicReference<Map<String, String>> headers = new AtomicReference<>(new HashMap<>());
        AtomicReference<Map<String, String>> responseHeaders = new AtomicReference<>(new HashMap<>());
        AtomicReference<String> body = new AtomicReference<>("no body");
        AtomicReference<String> responseBody = new AtomicReference<>("no body");
        AtomicInteger responseStatus = new AtomicInteger(600);

        request.onRequestBegin(theRequest -> {
            uri.set(theRequest.getURI().toString());
            method.set(theRequest.getMethod());
            group.append("Request URL: ")
                    .append(theRequest.getURI())
                    .append("\n");
            group.append("Method: ")
                    .append(theRequest.getMethod())
                    .append("\n");
        });
        request.onRequestHeaders(theRequest -> {
            Map<String, String> allureHeaders = new HashMap<>();
            group.append("Headers:").append("\n");
            List<String> obfuscatedHeaders = getObfuscatedHeaders();
            for (HttpField header : theRequest.getHeaders()) {
                if (!obfuscatedHeaders.contains(header.getName())) {
                    allureHeaders.put(header.getName(), String.join(" ,", header.getValues()));
                    group.append("\t")
                            .append(header.getName())
                            .append(" :")
                            .append(String.join(" ,", header.getValues()))
                            .append("\n");
                } else {
                    allureHeaders.put(header.getName(), "*****");
                    group.append("\t")
                            .append(header.getName())
                            .append(" : *****\n");
                }
            }
            headers.set(allureHeaders);
        });
        request.onRequestContent((theRequest, content) -> {
            group.append("Request body:\n");
            String decodedJson = StandardCharsets.UTF_8.decode(content).toString();
            try {
                String formattedJson = decodedJson.startsWith("[") ? new JSONArray(decodedJson).toString(4) : new JSONObject(decodedJson).toString(4);
                body.set(formattedJson);
                group.append(formattedJson);
            } catch (JSONException e) {
                body.set(decodedJson);
                group.append("Could not format response as JSON, plain text: ")
                        .append(decodedJson);
            }
            group.append("\n");
        });
        request.onRequestSuccess(theRequest -> {
            logger.info(group.toString());
            group.delete(0, group.length());
        });
        request.onRequestFailure((request1, throwable) -> {
            logger.info(group.toString());
            group.delete(0, group.length());
        });
        group.append("\n");
        request.onResponseBegin(theResponse -> {
            group.append("Response status: ")
                    .append(theResponse.getStatus()).append("\n");
        });
        request.onResponseHeaders(theResponse -> {
            Map<String, String> allureHeaders = new HashMap<>();
            group.append("Response headers:").append("\n");
            for (HttpField header : theResponse.getHeaders()) {
                allureHeaders.put(header.getName(), String.join(" ,", header.getValues()));
                group.append("\t")
                        .append(header.getName())
                        .append(" :")
                        .append(String.join(" ,", header.getValues()))
                        .append("\n");
            }
            responseHeaders.set(allureHeaders);
        });
        request.onResponseContent((theResponse, content) -> {
            group.append("Response body:\n");
            String decodedJson = StandardCharsets.UTF_8.decode(content).toString();
            try {
                String formattedJson = decodedJson.startsWith("[") ? new JSONArray(decodedJson).toString(4) : new JSONObject(decodedJson).toString(4);
                responseBody.set(formattedJson);
                group.append(formattedJson);
            } catch (JSONException e) {
                responseBody.set(decodedJson);
                group.append("Could not format response as JSON, plain text: ")
                        .append(decodedJson);
            }
            group.append("\n");
        });
        request.onResponseSuccess(theResponse -> {
            allureDecorate(uri, method, headers, body, responseStatus, responseHeaders, responseBody);
            logger.info(group.toString());
        });
        request.onResponseFailure((response, throwable) -> {
            allureDecorate(uri, method, headers, body, responseStatus, responseHeaders, responseBody);
            logger.info(group.toString());
        });
        return request;
    }

    private void allureDecorate(AtomicReference<String> uri, AtomicReference<String> method, AtomicReference<Map<String, String>> headers, AtomicReference<String> body, AtomicInteger responseStatus, AtomicReference<Map<String, String>> responseHeaders, AtomicReference<String> responseBody) {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();
        final HttpRequestAttachment requestAttachment = HttpRequestAttachment.Builder
                .create("Request", uri.get())
                .setMethod(method.get())
                .setHeaders(headers.get())
                .setBody(body.get())
                .build();

        processor.addAttachment(requestAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));

        final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(responseStatus.get())
                .setHeaders(responseHeaders.get())
                .setBody(responseBody.get())
                .build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));
    }
}
