package io.sbcts.web

import io.sbcts.service.NotificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class NotificationController {

    @Autowired NotificationService notificationService
    @SendTo("/topic/notifications")
    public Map send(String message) throws Exception {
        return ['message': message];
    }

    @GetMapping(value = "/api/notify")
    public getContexts(HttpServletRequest request, HttpServletResponse response) {
        notificationService.pollStatsAndNotify();
    }
}
