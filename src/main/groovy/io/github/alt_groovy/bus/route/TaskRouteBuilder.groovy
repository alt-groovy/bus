package io.github.alt_groovy.bus.route

import io.github.alt_groovy.bus.service.TaskService
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.processor.aggregate.GroupedBodyAggregationStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TaskRouteBuilder extends RouteBuilder {

    @Autowired
    protected TaskService taskService;


    @Value('${ALT_GROOVY_BUS_CLUSTERED:false}') private boolean ALT_GROOVY_BUS_CLUSTERED;
    @Value('${io.github.alt_groovy.bus.clustered:false}') private boolean clustered;
    @Value('${io.github.alt_groovy.bus.schedules.enabled:false}') private boolean taskSchedulesEnabled;
    @Value('${io.github.alt_groovy.bus.clustered.jms.request-timeout:3600000}') private long taskTimeout;

    private boolean isclustered(){
        return this.clustered || this.ALT_GROOVY_BUS_CLUSTERED
    }

    private String master(){
        return this.isclustered() ? 'master:service-bus:' : ''
    }

    @Override
    public void configure() throws Exception {

        onException(RuntimeException.class)
                .log(LoggingLevel.ERROR,'Failed task:${body.task}')
                .handled(true)
                .setBody().groovy("['task':request.body.task,'state':'FAILED']")
                .bean('taskStateService','logStateAndNotify')

        configureTaskLoadBalancer()
        configureTaskSynchroniser()
        configureTasksFromProperties()
    }


    public void configureTaskLoadBalancer(){

        from("jms:load-balancer?concurrentConsumers=100&requestTimeout=${taskTimeout}")
                .routeId("load-balancer-consumer")
                .log('Received load balanced task request for:${body[task]}')
                .toD('direct:${body[task]}-local')
    }

    public void configureTaskSynchroniser(){
        from("jms:synchroniser?concurrentConsumers=1&replyToConcurrentConsumers=1")
                .routeId("synchroniser-consumer")
                .log('Received synchronised task request for:${body[task]}')
                .toD('direct:${body[task]}-local')
    }

    public void configureTasksFromProperties(){

        Map<String, Object> properties = taskService.getApplicationProperties()

        def configuredSchedules = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.schedule.')}
        def configuredTimers = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.timer.')}
        def configuredSequences = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.sequence.')}
        def configuredSets = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.set.')}
        def configuredLabelGroups = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.label.group.')}
        def configuredSynchronisedTasks = properties.keySet()findAll{ key -> key.startsWith('io.github.alt_groovy.bus.service.synchronised.')}
        def configuredTasks = properties.keySet()findAll{ key ->
            key.startsWith('io.github.alt_groovy.bus.service.') && !(key.startsWith('io.github.alt_groovy.bus.service.timeout.')||key.startsWith('io.github.alt_groovy.bus.service.schedule.')||key.startsWith('io.github.alt_groovy.bus.service.timer.')||key.startsWith('io.github.alt_groovy.bus.service.sequence.')||key.startsWith('io.github.alt_groovy.bus.service.set.')||key.startsWith('io.github.alt_groovy.bus.service.synchronised.')||key.startsWith('io.github.alt_groovy.bus.service.label.group.'))
        }

        configuredTasks.each{ task ->
            configureTask(task,properties)
        }

        configuredSynchronisedTasks.each{ task ->
            configureTask(task, properties, true)
        }

        configuredSequences.each { sequence ->
            def String taskHandle = sequence.replace('io.github.alt_groovy.bus.service.sequence.','')
            def String endpointList = properties[sequence].toString().split(',').collect{ "direct://${it}"}.join(',')
            configureLoadBalancedTaskSequence(taskHandle, endpointList)
        }

        configuredSets.each { set ->
            def String taskHandle = set.replace('io.github.alt_groovy.bus.service.set.','')
            def String endpointList = properties[set].toString().split(',').collect{ "direct://${it}"}.join(',')
            configureLoadBalancedTaskSet(taskHandle, endpointList)
        }

        configuredSchedules.each { schedule ->
            def String taskHandle = schedule.replace('io.github.alt_groovy.bus.service.schedule.','')
            def String cronTab = properties[schedule]
            configureLeaderElectedTaskSchedule(taskHandle, cronTab)
        }
    }

    public void configureLeaderElectedRoute(String fromUri, String toUri){
        from("${master()}${fromUri}")
                .to(toUri)
    }

    public void configureTask(String task, Map properties, boolean isSynchronised = false){
        def taskDefinition = properties[task].toString()

        def String taskHandle = task.replace('io.github.alt_groovy.bus.service.','')
        if (isSynchronised) {
            taskHandle = taskHandle.replace('synchronised.','')
        }

        configureTaskRestEndpoint(taskHandle)

        if (isSynchronised){
            configureSynchronisedTask(taskHandle,taskDefinition)
        }else {
            configureLoadBalancedTask(taskHandle,taskDefinition)
        }
    }

    public void configureTaskRestEndpoint (String taskHandle){
        rest("${taskHandle}")
                .description("${taskHandle} trigger service")
                .consumes("application/json").produces("application/json")

                .post().id("${taskHandle}-rest")
                .description("Trigger task: ${taskHandle}")
                .route().log(LoggingLevel.INFO,"Web task request for:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'REQUESTING']")
                .bean('taskStateService','logStateAndNotify')
                .to("seda:${taskHandle}?exchangePattern=InOnly")

        from("seda:${taskHandle}")
                .routeId("${taskHandle}-asynchronous-in-only")
                .to("direct:${taskHandle}")
    }

    public void configureLoadBalancedTask(String taskHandle, String taskDefinition){

        from("direct:${taskHandle}")
                .routeId("${taskHandle}-load-balanced")
                .log(LoggingLevel.INFO,"Load balancing task request for:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'LOAD_BALANCING']")
                .bean('taskStateService','logStateAndNotify')
                .to("jms:load-balancer?exchangePattern=InOut&requestTimeout=${taskTimeout}")
                .log(LoggingLevel.INFO,'Task request returned:${body}')

        configureLocalTask(taskHandle,taskDefinition)
    }

    public void configureSynchronisedTask(String taskHandle, String taskDefinition){

        from("direct:${taskHandle}")
                .routeId("${taskHandle}-synchronised")
                .log(LoggingLevel.INFO,"Synchronising task request for:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'SYNCHRONISING']")
                .bean('taskStateService','logStateAndNotify')
                .to("jms:synchroniser?exchangePattern=InOut&requestTimeout=${taskTimeout}&synchronous=true")
                .log(LoggingLevel.INFO,'Task request returned:${body}')

        configureLocalTask(taskHandle,taskDefinition)
    }


    public void configureLocalTask(String taskHandle, String taskDefinition){
        from("direct:${taskHandle}-local")
                .routeId("${taskHandle}-local")
                .log("Starting task:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'STARTED','body':request.body]")
                .bean('taskStateService','logStateAndNotify')
                .to(taskDefinition)
                .setBody().groovy("request.getBody(String.class)")
                .log("Ended task:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':request.headers[org.apache.camel.component.exec.ExecBinding.EXEC_EXIT_VALUE]==0?'SUCCESS':'FAILED','body':request.body]")
                .bean('taskStateService','logStateAndNotify')

    }

    public void configureLoadBalancedTaskSequence(String taskHandle, String endpoints){
        from("direct:${taskHandle}")
                .routeId("${taskHandle}-load-balanced")
                .log(LoggingLevel.INFO,"Sending task sequence request for:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'LOAD_BALANCING']")
                .bean('taskStateService','logStateAndNotify')
                .to("jms:load-balancer?exchangePattern=InOut")
                .log(LoggingLevel.INFO,'Task sequence returned:${body}')

        from("direct:${taskHandle}-local")
                .routeId("${taskHandle}-local")
                .log(LoggingLevel.INFO,"Starting task sequence:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'STARTED']")
                .bean('taskStateService','logStateAndNotify')
                .multicast()
                .stopOnException()
                .aggregationStrategy(new GroupedBodyAggregationStrategy())
                .to(endpoints.split(','))
                .end()
                .log(LoggingLevel.INFO,"Ended task sequence:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':body.find{it.state =='FAILED'}?'FAILED':'SUCCESS']")
                .bean('taskStateService','logStateAndNotify')

        configureTaskRestEndpoint(taskHandle)
    }

    public void configureLoadBalancedTaskSet(String taskHandle, String endpoints){
        from("direct:${taskHandle}")
                .routeId("${taskHandle}-load-balanced")
                .log(LoggingLevel.INFO,"Sending task set request for:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'LOAD_BALANCING']")
                .bean('taskStateService','logStateAndNotify')
                .to("jms:load-balancer?exchangePattern=InOut")

        from("direct:${taskHandle}-local")
                .routeId("${taskHandle}-local")
                .log(LoggingLevel.INFO,"Starting task set:${taskHandle}")
                .setBody(constant(['task':'"+taskHandle+"','state':'STARTED']))
                .bean('taskStateService','logStateAndNotify')
                .multicast()
                .aggregationStrategy(new GroupedBodyAggregationStrategy())
                .parallelProcessing()
                .to(endpoints.split(','))
                .end()
                .log(LoggingLevel.INFO,"Ended task set:${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':body.find{it.state =='FAILED'}?'FAILED':'SUCCESS']")
                .bean('taskStateService','logStateAndNotify')

        configureTaskRestEndpoint(taskHandle)
    }

    public void configureLeaderElectedTaskSchedule(String taskHandle, String cronTab){
        String cronTabBase64 = Base64.getUrlEncoder().encodeToString(cronTab.getBytes())
        from("${master()}quartz:${taskHandle}-schedule-${cronTabBase64}?cron=${cronTab}")
                .routeId("${taskHandle} schedule ${cronTab}")
                .autoStartup(taskSchedulesEnabled)
                .setBody().constant(taskHandle)
        .choice().when().method('taskStateService','isNotActive')
                .log(LoggingLevel.INFO,"Triggering scheduled task :${taskHandle}")
                .setBody().groovy("['task':'${taskHandle}','state':'SCHEDULED','body':request.body]")
                .bean('taskStateService','logStateAndNotify')
                .to("seda:${taskHandle}-schedule-${cronTabBase64}")
        .otherwise()
                .log(LoggingLevel.INFO,"Skipping triggering of scheduled task :${taskHandle} is active")
        .end()

        from("seda:${taskHandle}-schedule-${cronTabBase64}?exchangePattern=InOnly")
                .routeId("${taskHandle} schedule ${cronTab} InOnly")
                .to("direct://${taskHandle}")
    }

}
