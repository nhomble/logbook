package org.zalando.logbook.jaxws;

import lombok.AllArgsConstructor;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Origin;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Set;

@AllArgsConstructor
public final class LogbookClientHandler implements SOAPHandler<SOAPMessageContext> {

    private final Logbook logbook;

    @Override
    public boolean handleMessage(final SOAPMessageContext context) {
        final Handler handler = new Handler(logbook, context);

        if (handler.isOutgoing()) {
            handler.handleRequest(Origin.LOCAL);
        } else {
            handler.handleResponse(Origin.REMOTE);
        }

        return true;
    }

    @Override
    public boolean handleFault(final SOAPMessageContext context) {
        // TODO log anyway?!
        return true;
    }

    @Override
    public void close(final MessageContext context) {
        // TODO anything to do here?!
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

}
