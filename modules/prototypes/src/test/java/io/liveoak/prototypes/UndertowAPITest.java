package io.liveoak.prototypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xnio.channels.StreamSourceChannel;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class UndertowAPITest {

    @Test
    public void testCreateServer() throws IOException {
        Undertow server = Undertow.builder()
                .addHttpListener(8580, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.getRequestMethod().equals(Methods.POST)) {
                            long len = exchange.getRequestContentLength();
                            if (len != -1) {
                                StreamSourceChannel channel = exchange.getRequestChannel();

                            }
                        }
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("Hello World");
                    }
                }).build();
        server.start();

        HttpGet get = new HttpGet("http://localhost:8580");
        String response = getRequest(get);
        System.out.println(response);
    }

    protected String getRequest(HttpGet get) throws IOException {
        System.err.println("DO GET - " + get.getURI());
        return request(get);
    }

    protected String request(HttpRequestBase request) throws IOException {
        CloseableHttpResponse result = httpClient.execute(request);

        System.err.println("=============>>>");
        System.err.println(result);

        HttpEntity resultEntity = result.getEntity();

        assertThat(resultEntity.getContentLength()).isGreaterThan(0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resultEntity.writeTo(baos);

        String resultStr = new String(baos.toByteArray());
        System.err.println(resultStr);
        System.err.println("\n<<<=============");
        return resultStr;
    }

    @Before
    public void setUpClient() throws Exception {
        RequestConfig cconfig = RequestConfig.custom().setSocketTimeout(500000).build();
        this.httpClient = HttpClients.custom().disableContentCompression().setDefaultRequestConfig(cconfig).build();
    }

    @After
    public void tearDownClient() throws Exception {
        this.httpClient.close();
    }

    protected CloseableHttpClient httpClient;
}
