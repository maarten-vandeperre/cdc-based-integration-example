// camel-k: language=java dependency=camel-quarkus-rest dependency=camel-http
// kamel run src/main/java/demo/integrations/DeveloperHubProxyCamelK.java
// kamel get
// kamel log developer-hub-proxy-camel-k

package demo.integrations;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;

public class DeveloperHubProxyCamelK  extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Define the REST endpoint that acts as a proxy
        rest("/proxy")
                .get()
                .param()
                .name("url")
                .type(RestParamType.query)
                .required(true)
                .description("The URL to be proxied")
                .endParam()
                .to("direct:proxyHandler");

        // Define the route that handles the proxy logic
        from("direct:proxyHandler")
                .process(exchange -> {
                    String url = exchange.getIn().getHeader("url", String.class);
                    exchange.getIn().setHeader(Exchange.HTTP_URI, url);
                    log.info("Calling URL: " + url);
                    log.info("Request Headers: " + exchange.getIn().getHeaders());
                })
                .toD("${header.url}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .process(exchange -> {
                    // Log the response code
                    Integer responseCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                    log.info("Response Code: " + responseCode);

                    // Log the response headers
                    log.info("Response Headers: " + exchange.getIn().getHeaders());

                    // Remove the `url` parameter from the headers to prevent any leakage
                    exchange.getIn().removeHeader("url");
                })
                .log("Proxying to URL: ${header.url}");
    }
}