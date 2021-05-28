package io.sbcts.web

import io.sbcts.service.TaskService
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
