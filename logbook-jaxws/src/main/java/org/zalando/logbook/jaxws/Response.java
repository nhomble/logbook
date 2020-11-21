package org.zalando.logbook.jaxws;

import lombok.AllArgsConstructor;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import javax.annotation.Nullable;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.soap.SOAPMessage.CHARACTER_SET_ENCODING;
import static javax.xml.ws.handler.MessageContext.HTTP_RESPONSE_CODE;
import static org.zalando.fauxpas.FauxPas.throwingUnaryOperator;

@AllArgsConstructor
final class Response implements HttpResponse {

    private final AtomicReference<State> state = new AtomicReference<>(new Unbuffered());

    private final SOAPMessageContext context;
    private final Origin origin;

    @Override
    public String getProtocolVersion() {
        return "HTTP/1.1";
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public int getStatus() {
        return Optional.ofNullable(context.get(HTTP_RESPONSE_CODE))
                .map(Integer.class::cast)
                .orElse(0);
    }

    @Override
    public HttpHeaders getHeaders() {
        @SuppressWarnings("unchecked") final Map<String, List<String>> headers =
                (Map<String, List<String>>) context.get(MessageContext.HTTP_RESPONSE_HEADERS);

        // TODO OMG
        headers.remove(null);

        return HttpHeaders.of(headers);
    }

    @Override
    @Nullable
    public String getContentType() {
        // TODO use response header
        return null;
    }

    @Override
    public Charset getCharset() {
        return Optional.ofNullable(context.get(CHARACTER_SET_ENCODING))
                .map(Object::toString)
                .map(Charset::forName)
                .orElse(UTF_8);
    }

    @Override
    public HttpResponse withBody() {
        state.updateAndGet(State::with);
        return this;
    }

    @Override
    public HttpResponse withoutBody() {
        state.updateAndGet(State::without);
        return this;
    }

    @Override
    public byte[] getBody() {
        return state.updateAndGet(throwingUnaryOperator(state ->
                state.buffer(context.getMessage()))).getBody();
    }

}
