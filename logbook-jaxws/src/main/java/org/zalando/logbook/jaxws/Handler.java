package org.zalando.logbook.jaxws;

import lombok.AllArgsConstructor;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.logbook.Origin;

import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;

import static javax.xml.ws.handler.MessageContext.MESSAGE_OUTBOUND_PROPERTY;

@AllArgsConstructor
final class Handler {

    private static final String ATTRIBUTE = ResponseProcessingStage.class.getName();

    private final Logbook logbook;
    private final SOAPMessageContext context;

    boolean isIncoming() {
        return !isOutgoing();
    }

    boolean isOutgoing() {
        return (boolean) context.get(MESSAGE_OUTBOUND_PROPERTY);
    }

    void handleRequest(final Origin origin) {
        final Request request = new Request(context, origin);
        final ResponseProcessingStage stage = write(request);
        memorize(stage);
    }

    void handleResponse(final Origin origin) {
        final ResponseProcessingStage stage = recall();
        write(stage, new Response(context, origin));
    }

    private void memorize(final ResponseProcessingStage stage) {
        context.put(ATTRIBUTE, stage);
    }

    private ResponseProcessingStage recall() {
        return (ResponseProcessingStage) context.remove(ATTRIBUTE);
    }

    private ResponseProcessingStage write(final HttpRequest request) {
        try {
            return logbook.process(request).write();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(final ResponseProcessingStage stage, final HttpResponse response) {
        try {
            stage.process(response).write();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
