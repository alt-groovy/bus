package io.github.alt_groovy.bus.web


import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class HtmlController {

    @GetMapping(value = "/index.html")
    public String  getIndex(HttpServletRequest request, HttpServletResponse response, Model model) {
        return 'index'
    }

}
