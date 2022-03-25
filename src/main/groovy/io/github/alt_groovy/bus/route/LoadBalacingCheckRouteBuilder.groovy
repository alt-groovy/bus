package io.github.alt_groovy.bus.route


import org.apache.camel.builder.RouteBuilder
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
@Profile('load-balance-test')
class LoadBalacingCheckRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

         from("master:job-server:timer:timer1?period=2000&repeatCount=25")
        .routeId("Periodic Add to Distributed Queue")
                .setBody().groovy("System.currentTimeMillis()")
                .log('Put On Queue -> ${body}')
                .to("jms:distributed-queue")


        from("jms:distributed-queue?concurrentConsumers=1")
                .routeId("Listen On Distributed Queue")
                .log('Distributed Queue found : ${body}')

    }
}