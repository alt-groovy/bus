package io.sbcts.route

import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile('tac-proxy')
class CacheRefreshRouteBuilder extends RouteBuilder {

    @Value('${server.context}') private String context;
    @Value('${tac.cache.refresh.schedule:0+0+8-18+?+*+MON-FRI}') private String cacheRefreshSchedule
    @Override
    public void configure() throws Exception {

        from("timer:tac-cache-refresh?repeatCount=1")
                .routeId("Initial refresh of TAC Job Cache timer")
                .log(LoggingLevel.INFO, "Triggering initial refresh of TAC Job Cache")
                .to("direct:refreshCache")

        from("quartz:tac-cache-refresh?cron=${cacheRefreshSchedule}")
                .routeId("Scheduled refresh of TAC Job Cache timer")
                .log(LoggingLevel.INFO, "Triggering scheduled refresh of TAC Job Cache")
                .to("direct:refreshCache")

        from("direct:refreshCache")
                .routeId("Refresh TAC Job Cache")
                .bean('jobExecutionServiceTac','refreshTaskCaches')
    }
}