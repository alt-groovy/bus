package io.github.alt_groovy.bus.config


import org.apache.activemq.artemis.core.config.ClusterConnectionConfiguration

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl

import org.apache.activemq.artemis.core.server.ActiveMQServer
import org.apache.activemq.artemis.core.server.ActiveMQServers
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory

import org.apache.camel.Component
import org.apache.camel.component.jms.JmsComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


import javax.jms.JMSException


@Configuration
public class ActiveMQArtemisBroker {

    @Value('${artemis.acceptor.uri}') private String acceptorUri;
    @Value('${artemis.connector.uri}') private String connectorUri;
    @Value('${artemis.journal.directory}') private String journalDir;
    @Value('${artemis.user}') private String username;
    @Value('${artemis.password}') private String password;
    @Value('${camel.component.jms.request-timeout:20000}') private long requestTimeout;

    @Bean
    ActiveMQServer activeMQServer(){

        org.apache.activemq.artemis.core.config.Configuration config = new ConfigurationImpl();
        config.setPersistenceEnabled(false)
                .setJournalDirectory(journalDir)
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("artemis",acceptorUri)
                .addConnectorConfiguration("artemis",acceptorUri)

//        connectorUri.split(',').eachWithIndex{ String uri, int i -> config.addAcceptorConfiguration("${i}",uri) }

        connectorUri.split(',').eachWithIndex{ String uri, int i -> config.addConnectorConfiguration("connector-${i}",uri) }

        ClusterConnectionConfiguration clusterConnectionConfiguration = new ClusterConnectionConfiguration();
        clusterConnectionConfiguration.setMaxHops(1)
        clusterConnectionConfiguration.setStaticConnectors([0..connectorUri.split(',').size()].collect { "connector-${it}".toString()})
        clusterConnectionConfiguration.setName("artemis-cluster")
        clusterConnectionConfiguration.setAddress("preprod")
        clusterConnectionConfiguration.setMessageLoadBalancingType(MessageLoadBalancingType.STRICT)
        clusterConnectionConfiguration.setConnectorName('artemis')
        clusterConnectionConfiguration.setDuplicateDetection(true)
        config.setClusterUser(username)
        config.setClusterPassword(password)
        config.addClusterConfiguration(clusterConnectionConfiguration)

//        CoreQueueConfiguration coreQueueConfiguration = new CoreQueueConfiguration()
//        coreQueueConfiguration.setName('distributed-queue')
//                .setAddress('preprod')
//        .setRoutingType(RoutingType.ANYCAST)
//
//        config.addQueueConfiguration(coreQueueConfiguration)

        ActiveMQServer server = ActiveMQServers.newActiveMQServer(config);
        server.start()

        return server;
    }

    @Bean
    public Component jms() throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
        cf.setBrokerURL(acceptorUri);
        cf.setUser(username);
        cf.setPassword(password);

        JmsComponent jms = new JmsComponent()
        jms.setConnectionFactory(cf)
        jms.setRequestTimeout(requestTimeout)

        return jms;
    }
}