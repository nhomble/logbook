package org.zalando.logbook.jaxws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zalando.logbook.DefaultHttpLogFormatter;
import org.zalando.logbook.DefaultSink;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Precorrelation;

import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO split client and server test
final class LogbookHandlerTest<Client extends BookService & BindingProvider> {

    private final Endpoint server = Endpoint.create(new BookServiceImpl());

    @SuppressWarnings("unchecked")
    private final Client client = (Client) Service.create(
            new URL("file:src/test/resources/books.wsdl"),
            new QName("http://jaxws.logbook.zalando.org/", "BookServiceImplService")
    ).getPort(BookService.class);

    private final HttpLogWriter writer = mock(HttpLogWriter.class);

    public LogbookHandlerTest() throws MalformedURLException {
        final Logbook logbook = Logbook.builder()
                .sink(new DefaultSink(
                        new DefaultHttpLogFormatter(),
                        writer
                ))
                .build();

        register(server.getBinding(), new LogbookServerHandler(logbook));
        register(client.getBinding(), new LogbookClientHandler(logbook));
    }

    private static void register(final Binding binding, final Handler<SOAPMessageContext> handler) {
        @SuppressWarnings("rawtypes") final List<Handler> handlers = binding.getHandlerChain();
        handlers.add(handler);
        binding.setHandlerChain(handlers);
    }

    @BeforeEach
    void defaultBehavior() {
        when(writer.isActive()).thenReturn(true);
    }

    @BeforeEach
    void start() {
        server.publish("http://localhost:8080/books");
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void test() throws IOException {
        final Book book = client.getBook(1);
        assertThat(book.getName()).isEqualTo("Logbook");

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer, times(2)).write(any(Precorrelation.class), captor.capture());

        assertThat(captor.getAllValues()).element(0).asString().contains("Outgoing Request");
        assertThat(captor.getAllValues()).element(1).asString().contains("Incoming Request");
    }

    // TODO client request
    // TODO client response
    // TODO server request
    // TODO server response

    // TODO with/without body

    // TODO faults

}
