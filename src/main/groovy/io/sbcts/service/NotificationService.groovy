package io.sbcts.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class NotificationService {
    @Value('${notifications.enabled:false}') private boolean notificationsEnabled;

    @Autowired SimpMessagingTemplate simpMessagingTemplate;


    public void notifyTaskState(Map taskState){
        sendNotification([uuid: UUID.randomUUID().toString(), type:'TaskState', taskState:taskState])
    }

    public void sendNotification(Map notification){
        simpMessagingTemplate.convertAndSend('/topic/notifications',notification)
    }
}
