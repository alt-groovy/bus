package io.github.alt_groovy.bus.web

import io.github.alt_groovy.bus.service.TaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api")
class TaskController extends AbstractController {

    @Autowired TaskService taskService

    @GetMapping(value = "/tasks")
    public getJobs(HttpServletRequest request, HttpServletResponse response ) {
        try {
            return taskService.getTasks();
        } catch (Exception e) {
            return errorRequest(request,response,e)
        }
    }

}
