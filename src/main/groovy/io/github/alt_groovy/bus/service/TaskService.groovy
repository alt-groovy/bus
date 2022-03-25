package io.github.alt_groovy.bus.service


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.stereotype.Service

@Service
class TaskService {

    Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired protected Environment environment;
    @Autowired protected TaskStateService taskStateService

    public Map getApplicationProperties (){
        Map<String, Object> properties = new HashMap();
        for(Iterator it = environment.getPropertySources().iterator(); it.hasNext(); ) {
            PropertySource propertySource = (PropertySource) it.next();
            if (propertySource instanceof MapPropertySource) {
                properties.putAll(((MapPropertySource) propertySource).getSource());
            }
        }

        return properties;
    }

    public Map getTaskMap() {

        Map<String, Object> properties = getApplicationProperties();

        def configuredSchedules = properties.keySet().findAll{ key -> key.startsWith('task.schedule.')}
        def configuredTimers = properties.keySet().findAll{ key -> key.startsWith('task.timer.')}
        def configuredSequences = properties.keySet().findAll{ key -> key.startsWith('task.sequence.')}
        def configuredSets = properties.keySet().findAll{ key -> key.startsWith('task.set.')}
        def configuredLabelGroups = properties.keySet().findAll{ key -> key.startsWith('task.label.group.')}
        def configuredSynchronisedTasks = properties.keySet().findAll{ key -> key.startsWith('task.synchronised.')}
        def configuredTasks = properties.keySet().findAll{ key ->
            key.startsWith('task.') && !(key.startsWith('task.schedule.')||key.startsWith('task.timer.')||key.startsWith('task.sequence.')||key.startsWith('task.set.')||key.startsWith('task.synchronised.')||key.startsWith('task.label.group.'))
        }

        def Map taskMap = [root:[name:'root',parents:[], children:[]]]


        setChildren(taskMap,properties,configuredLabelGroups,'task.label.group.')
        setChildren(taskMap,properties,configuredSequences,'task.sequence.')
        setChildren(taskMap,properties,configuredSets,'task.set.')

        setParents(taskMap,properties,configuredLabelGroups,'task.label.group.')
        setParents(taskMap,properties,configuredSequences,'task.sequence.')
        setParents(taskMap,properties,configuredSets,'task.set.')
        setOrphans(taskMap)

        setType(taskMap,configuredLabelGroups,'task.label.group.','label')
        setType(taskMap,configuredSequences,'task.sequence.','sequence')
        setType(taskMap,configuredSets,'task.set.','set')
        setType(taskMap,configuredSynchronisedTasks,'task.synchronised.','synchronised')
        setType(taskMap,configuredTasks,'task.','task')


        setSchedules(taskMap,properties,configuredSchedules,'task.schedule.')

        return taskMap
    }

    public Map getTasks(String taskCode) {
        def tasks = [:]
        def taskRefMap = getTaskMap()
        def taskTree =  getTaskTree (taskRefMap,[:], 'root')
        tasks['map'] = taskRefMap
        tasks['tree'] = taskTree
        return tasks
    }

    public Map getTaskTree (Map taskRefMap,  Map taskMap, String taskCode) {
        def taskNode = taskMap[taskCode]
        if (taskNode == null){

            taskNode = [name:taskCode]
            taskMap[taskCode] = taskNode

            def children = []
            taskRefMap[taskCode]?.children.each { child ->
                children << getTaskTree (taskRefMap,taskMap,child)
            }

            taskNode['children'] = children.sort { child -> child.name}

        }

        return taskNode
    }

    public void setType(Map taskMap, Set keys, String prefix, String type){
        keys.each { propertyKey ->
            def String taskHandle = propertyKey.replace(prefix,'')
            if (taskMap[taskHandle] == null) {
                taskMap[taskHandle] = [name:taskHandle, type: '', children: [], parents:[], schedules:[],statelist:(taskStateService.getStateMap()[taskHandle])?:[['state':'UNKNOWN', 'timestamp':System.currentTimeMillis()]]]
            }
            taskMap[taskHandle]['type'] = type
            taskMap[taskHandle]['statelist'] = (taskStateService.getStateMap()[taskHandle])?:[['state':'UNKNOWN', 'timestamp':System.currentTimeMillis()]]
        }
    }

    public void setSchedules(Map taskMap,  Map properties, Set keys, String prefix){
        keys.each { propertyKey ->
            def String taskHandle = propertyKey.replace(prefix,'')

            if (taskMap[taskHandle]['schedules'] == null){
                taskMap[taskHandle]['schedules'] = []
            }
            taskMap[taskHandle]['schedules'] << properties[propertyKey].toString()
        }
    }

    public void setChildren(Map taskMap, Map properties, Set keys, String prefix){
        keys.each { propertyKey ->
            def String taskHandle = propertyKey.replace(prefix,'')
            def endpointList = properties[propertyKey]?.toString().replace(' ','').split(',')

            taskMap[taskHandle] = [name:taskHandle, type: '', children: endpointList, parents:[], schedules:[]]
        }
    }

    public void setParents(Map taskMap, Map properties, Set tasks, String prefix){
        tasks.each { propertyKey ->
            def String taskHandle = propertyKey.replace(prefix,'')
            def endpointList = properties[propertyKey].toString().replace(' ','').split(',')
            endpointList.each { child ->
                if (taskMap[child] != null){
                    taskMap[child]['parents'] << taskHandle
                }
            }

        }
    }

    public void setOrphans(Map taskMap){
        taskMap.keySet().each { task ->

            if (!'root'.equals(task) &&taskMap[task].parents.size() == 0){
                taskMap[task].parents << 'root'
                taskMap['root'].children << task
            }
        }
    }

}
