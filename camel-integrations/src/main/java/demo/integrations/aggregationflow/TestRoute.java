package demo.integrations.aggregationflow;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class TestRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        String camelRouteName = "enrich_contracts";
        boolean enabled = Boolean.valueOf(getContext().resolvePropertyPlaceholders("{{feature.flag.camel_routes.enrich_contracts.enabled}}"));

        if (enabled) {
            System.out.println("Camel route " + camelRouteName + " enabled");
            processCamelRoutes();
        } else {
            System.out.println("Camel route " + camelRouteName + " disabled");
        }
    }

    private void processCamelRoutes() {
        from("direct:start")
                .to("mock:result");
    }
}
