package configuration;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateLoggingInterceptor.class);

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

    private static final String EMPTY = "-empty-";
    private static final String DISABLED = "-disabled-";
    private static final String NOT_LOGGABLE = "-not-loggable-";

    private static final String OBFUSCATION_VALUE = "****";


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (logger.isInfoEnabled()) {

            HttpHeaders requestHeaders = request.getHeaders();
            boolean loggableRequestBody = isLoggableBody(requestHeaders);

            LoggingConsumer consumer = new StringJoinerLoggingConsumer();

            boolean logTimeTracingEnabled = isLogTimeTracingEnabled();
            StopWatch stopWatch = new StopWatch();

            try {

                if (logTimeTracingEnabled) {
                    stopWatch.start();
                }
                ClientHttpResponse result = execution.execute(request, body);

                if (logTimeTracingEnabled) {
                    stopWatch.stop();
                }

                HttpHeaders responseHeaders = result.getHeaders();
                boolean loggableResponseBody = isLoggableBody(responseHeaders);

                if (isLogResponseBodyEnabled() && loggableResponseBody) {
                    BufferedResponseWrapper wrapper = new BufferedResponseWrapper(result);
                    traceRequestResponse(request, body, wrapper, loggableRequestBody, true, logTimeTracingEnabled, stopWatch, consumer);
                    logger.info(consumer.getLog());

                    return wrapper;
                } else {
                    traceRequestResponse(request, body, result, loggableRequestBody, loggableResponseBody, logTimeTracingEnabled, stopWatch, consumer);
                    logger.info(consumer.getLog());

                    return result;
                }

            } catch (IOException ex) {
                if (logTimeTracingEnabled && stopWatch.isStarted()) {
                    stopWatch.stop();
                }

                traceException(request, body, ex, consumer, logTimeTracingEnabled, stopWatch, loggableRequestBody);
                logger.warn(consumer.getLog());
                throw ex;
            }
        } else {
            return execution.execute(request, body);
        }
    }

    protected void traceException(HttpRequest request, byte[] body, Throwable ex, LoggingConsumer consumer, boolean logTimeTracingEnabled, StopWatch stopWatch,
                                  boolean loggableRequestBody) {
        consumer.appendLog("\n===== Start ERROR ====");
        traceRequest(request, body, loggableRequestBody, consumer);
        consumer.appendLog("\tException was thrown when executing request class[%s], message=[%s]", ex.getClass(), ex.getMessage());
        traceTiming(logTimeTracingEnabled, stopWatch, consumer);
        consumer.appendLog("===== End   ERROR ====");
    }

    protected void traceRequestResponse(HttpRequest request, byte[] body, ClientHttpResponse response, boolean loggableRequestBody, boolean loggableResponseBody,
                                        boolean logTimeTracingEnabled, StopWatch stopWatch, LoggingConsumer consumer) throws IOException {
        consumer.appendLog("\n===== START =====");
        traceRequest(request, body, loggableRequestBody, consumer);
        traceResponse(response, loggableResponseBody, logTimeTracingEnabled, stopWatch, consumer);
        consumer.appendLog("===== END   =====");
    }

    private void traceTiming(boolean logTimeTracingEnabled, StopWatch stopWatch, LoggingConsumer consumer) {
        if (logTimeTracingEnabled) {
            logSafeLine(consumer, "Response Time", stopWatch::toString);
        }
    }

    protected void traceRequest(HttpRequest request, byte[] body, boolean loggableRequestBody, LoggingConsumer consumer) {
        HttpHeaders headers = request.getHeaders();

        consumer.appendLog(" ===== Request =====");
        logSafeLine(consumer, "Uri", request::getURI);
        logSafeLine(consumer, "Method", request::getMethod);
        logSafeLine(consumer, "Request-Id", () -> headers.get("request-id"));

        logRequestHeaders(headers, consumer);
        logRequestBody(consumer, loggableRequestBody, body);
    }

    protected void traceResponse(ClientHttpResponse response, boolean loggableResponseBody, boolean logTimeTracingEnabled, StopWatch stopWatch, LoggingConsumer consumer) throws IOException {
        HttpHeaders headers = response.getHeaders();

        consumer.appendLog(" ===== Response =====");
        logLine(consumer, "Status", response::getRawStatusCode);
        traceTiming(logTimeTracingEnabled, stopWatch, consumer);

        logResponseHeaders(headers, consumer);

        logResponseBody(consumer, loggableResponseBody, response.getBody());

    }

    protected void logRequestBody(LoggingConsumer consumer, boolean loggableRequestBody, byte[] body) {

        consumer.appendLog("\t==== Request Body ====");
        if (isLogRequestBodyEnabled()) {
            if (ArrayUtils.isEmpty(body)) {
                consumer.appendLog("\t%s", EMPTY);
            } else if (loggableRequestBody) {
                consumer.appendLog("\t%s", new String(body, StandardCharsets.UTF_8));
            } else {
                consumer.appendLog("\t%s", NOT_LOGGABLE);
            }
        } else {
            consumer.appendLog("\t%s", DISABLED);
        }
    }

    protected void logResponseBody(LoggingConsumer consumer, boolean loggableResponseBody, InputStream bodyStream) throws IOException {
        consumer.appendLog("\t==== Response Body ====");
        if (!isLogResponseBodyEnabled()) {
            consumer.appendLog("\t%s", DISABLED);
            return;
        }
        if (!loggableResponseBody) {
            consumer.appendLog("\t%s", NOT_LOGGABLE);
            return;
        }

        int currentLine = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream))) {
            String line = reader.readLine();
            if (line != null) {
                consumer.appendLog("\t%s", line);
                while ((line = reader.readLine()) != null && currentLine < getLogMaxResponseLines()) {
                    currentLine++;
                    consumer.appendLog("\t%s", line);
                }
            } else {
                consumer.appendLog("\t%s", EMPTY);
            }

        } catch (IOException ex) {
            consumer.appendLog("Could  not log response body.");
            throw ex;
        }
    }

    protected boolean isLoggableBody(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType != null) {
            return LOGGABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith);
        }

        return false;
    }

    protected void logResponseHeaders(HttpHeaders headers, LoggingConsumer consumer) {
        consumer.appendLog("\t===== Response Headers ====");
        logHeaders(headers, consumer, getLogResponseHeaders());
    }

    protected void logRequestHeaders(HttpHeaders headers, LoggingConsumer consumer) {
        consumer.appendLog("\t===== Request Headers ====");
        logHeaders(headers, consumer, getLogRequestHeaders());

    }

    protected void logHeaders(HttpHeaders headers, LoggingConsumer consumer, List<String> loggedHeaders) {
        if (!loggedHeaders.isEmpty()) {
            headers.forEach((header, values) -> logHeaderLine(loggedHeaders, consumer, header, values));
        } else {
            consumer.appendLog("\t%s", DISABLED);
        }
    }

    protected void logHeaderLine(List<String> loggedHeaders, LoggingConsumer consumer, String header, List<String> values) {
        String caseInsensitiveHeader = header.toLowerCase();
        if (getLogAllHeaders()) {
            Optional.ofNullable(values)
                    .filter(CollectionUtils::isNotEmpty)
                    .map(v -> createHeaderLine(header, caseInsensitiveHeader, v))
                    .ifPresent(consumer::appendLog);
        } else {

            Optional.ofNullable(values)
                    .filter(CollectionUtils::isNotEmpty)
                    .filter(v -> loggedHeaders.contains(caseInsensitiveHeader))
                    .filter(v -> !getBlackListHeaders().contains(caseInsensitiveHeader))
                    .map(v -> createHeaderLine(header, caseInsensitiveHeader, v))
                    .ifPresent(consumer::appendLog);
        }
    }

    protected String createHeaderLine(String header, String caseInsensitiveHeader, List<String> headerValue) {
        Object transformedHeaders = obfuscateHeadersIfRequired(caseInsensitiveHeader, headerValue);
        return String.format("\tHeader[%s]: %s", header, transformedHeaders);
    }

    protected Object obfuscateHeadersIfRequired(String header, List<String> headerValue) {
        List<String> obfuscatedHeaders = getObfuscatedHeaders();
        return obfuscatedHeaders.contains(header) ? OBFUSCATION_VALUE : headerValue;
    }

    protected void logLine(LoggingConsumer consumer, String key, LogValueSupplier supplier) throws IOException {
        try {
            Object theValue = supplier.get();

            consumer.appendLog("\t%s: %s", key, theValue);
        } catch (IOException ex) {
            consumer.appendLog("Could not log for key=[%s] because an exception was thrown.", key);
            throw ex;
        }
    }

    protected void logSafeLine(LoggingConsumer consumer, String key, Supplier<Object> supplier) {
        Object theValue = supplier.get();

        consumer.appendLog("\t%s: %s", key, theValue);
    }

    protected boolean isLogRequestBodyEnabled() {
        return true;
    }

    protected boolean isLogTimeTracingEnabled() {
        return true;
    }

    protected boolean isLogResponseBodyEnabled() {
        return true;
    }

    protected boolean getLogAllHeaders() {
        return true;
    }

    protected int getLogMaxResponseLines() {
        return 10;
    }

    protected List<String> getLogRequestHeaders() {
        return Arrays.asList("accept", "content-type", "content-length", "cache-control", "accept-charset", "accept-encoding", "user-agent", "connection");
    }

    protected List<String> getLogResponseHeaders() {
        return Arrays.asList("accept", "content-type", "content-length", "cache-control", "accept-charset", "accept-encoding", "user-agent", "connection");
    }

    protected List<String> getBlackListHeaders() {
        return List.of();
    }

    protected List<String> getObfuscatedHeaders() {
        return List.of("authorization");
    }

    private static class BufferedResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse response;

        @Nullable
        private byte[] body;

        public BufferedResponseWrapper(ClientHttpResponse response) {
            this.response = response;
        }

        @Override
        public HttpStatus getStatusCode() throws IOException {
            return this.response.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return this.response.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return this.response.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.response.getHeaders();
        }

        @Override
        public InputStream getBody() throws IOException {
            if (this.body == null) {
                this.body = StreamUtils.copyToByteArray(this.response.getBody());
            }
            return new ByteArrayInputStream(this.body);
        }

        @Override
        public void close() {
            this.response.close();
        }
    }

    @FunctionalInterface
    interface LogValueSupplier<T> {
        T get() throws IOException;
    }

    interface LoggingConsumer {
        void appendLog(String format, Object... args);

        void appendLog(String message);

        String getLog();
    }

    protected static class StringJoinerLoggingConsumer implements LoggingConsumer {

        private StringJoiner joiner;

        protected StringJoinerLoggingConsumer() {
            this(new StringJoiner("\n"));
        }

        protected StringJoinerLoggingConsumer(StringJoiner joiner) {
            this.joiner = joiner;
        }

        @Override
        public void appendLog(String format, Object... args) {
            joiner.add(String.format(format, args));
        }

        @Override
        public void appendLog(String message) {
            joiner.add(message);
        }

        @Override
        public String getLog() {
            return joiner.toString();
        }
    }
}
