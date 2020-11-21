package org.zalando.logbook.jaxws;

import lombok.AllArgsConstructor;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;

import javax.annotation.Nullable;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.soap.SOAPMessage.CHARACTER_SET_ENCODING;
import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import static javax.xml.ws.handler.MessageContext.HTTP_REQUEST_HEADERS;
import static javax.xml.ws.handler.MessageContext.HTTP_REQUEST_METHOD;
import static javax.xml.ws.handler.MessageContext.QUERY_STRING;
import static lombok.AccessLevel.PRIVATE;
import static org.zalando.fauxpas.FauxPas.throwingUnaryOperator;

@AllArgsConstructor(access = PRIVATE)
final class Request implements HttpRequest {

    private final AtomicReference<State> state = new AtomicReference<>(new Unbuffered());

    private final SOAPMessageContext context;
    private final Origin origin;

    private final URI baseUri;

    public Request(final SOAPMessageContext context, final Origin origin) {
        this(context, origin, URI.create(""));
                // TODO URI.create(context.get(ENDPOINT_ADDRESS_PROPERTY).toString()));
    }

    @Override
    public String getProtocolVersion() {
        return "HTTP/1.1";
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public String getRemote() {
        // TODO implement
        return "";
    }

    @Override
    public String getMethod() {
        // TODO is this correct?!
        return "POST";
    }

    @Override
    public String getScheme() {
        return baseUri.getScheme();
    }

    @Override
    public String getHost() {
        return baseUri.getHost();
    }

    @Override
    public Optional<Integer> getPort() {
        final int port = baseUri.getPort();
        return port == -1 ? Optional.empty() : Optional.of(port);
    }

    @Override
    public String getPath() {
        return baseUri.getPath();
    }

    @Override
    public String getQuery() {
        return Optional.ofNullable(context.get(QUERY_STRING)).orElse("").toString();
    }

    @Override
    public HttpHeaders getHeaders() {
        return HttpHeaders.of(getRawHeaders());
    }

    @Override
    @Nullable
    public String getContentType() {
        return Optional.ofNullable(getRawHeaders().get("Content-Type"))
                .map(Iterable::iterator)
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .orElse(null);
    }

    private Map<String, List<String>> getRawHeaders() {
        @SuppressWarnings("unchecked") @Nullable final Map<String, List<String>> headers =
                (Map<String, List<String>>) context.get(HTTP_REQUEST_HEADERS);

        return Optional.ofNullable(headers).orElse(Collections.emptyMap());
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(context.get(CHARACTER_SET_ENCODING))
                .map(Object::toString)
                .map(Charset::forName)
                .orElse(UTF_8);
    }

    @Override
    public HttpRequest withBody() {
        state.updateAndGet(State::with);
        return this;
    }

    @Override
    public HttpRequest withoutBody() {
        state.updateAndGet(State::without);
        return this;
    }

    @Override
    public byte[] getBody() {
        return state.updateAndGet(throwingUnaryOperator(state ->
                state.buffer(context.getMessage()))).getBody();
    }

}