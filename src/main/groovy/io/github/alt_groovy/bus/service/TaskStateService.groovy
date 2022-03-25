package io.github.alt_groovy.bus.service


import org.apache.camel.ProducerTemplate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment

import org.springframework.stereotype.Service


@Service
class TaskStateService {

    Logger logger = LoggerFactory.getLogger(TaskStateService.class);

    @Autowired protected Environment environment;
    @Autowired private NotificationService notificationService

    @Value('${server.task.execution.history.size:10}') private int stateQueueSize;
    @Autowired private ProducerTemplate producer;

    protected Map taskStates = [:]

    public void logStateAndNotify(Map map) {
        def taskState = logState(map.task,map.state)
        notificationService.notifyTaskState(taskState)
    }

    public Map logState(String taskHandle, String state){
        Map distibutedStateMap = getStateMap()

        def List stateList = (List)distibutedStateMap.get(taskHandle)
        def Map currentState = ['task': taskHandle, 'state':state, timestamp : System.currentTimeMillis()]
        if (stateList == null){
            stateList = [currentState]
            distibutedStateMap.put(taskHandle,stateList)
        } else {
            stateList.push(currentState)
            if (stateList.size() > stateQueueSize){
                stateList.removeLast()
            }
        }


        putStateMap(distibutedStateMap)

        return currentState
    }

    public boolean isNotActive(String taskHandle){
        boolean isActive = true;

        Map distibutedStateMap = getStateMap()
        def List stateList = (List)distibutedStateMap.get(taskHandle)
        isActive = stateList != null && stateList?.size() > 0 && ['SYNCHRONISING','REQUESTED','SCHEDULED','LOAD_BALANCING','STARTED'].contains(stateList[0].state)
        logger.debug("Statelist for ${taskHandle} is ${stateList}")
        logger.debug("Job ${taskHandle} is ${isActive ? 'active' : 'inactive'} with state ${(stateList != null && stateList?.size() > 0) ? stateList[0].state : 'UNKNOWN'}")
        return !isActive
    }

    public Map getStateMap(){
//        IMap distibutedStateMap = producer.sendBody("direct:${context}-get-job-state-map",'')
        return taskStates;
    }

    public void putStateMap(Map map){
//        producer.sendBody("direct:${context}-put-job-state-map",distibutedStateMap)
        //do nothing for local
    }


}
