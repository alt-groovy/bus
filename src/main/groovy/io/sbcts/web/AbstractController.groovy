package io.sbcts.web

import org.springframework.http.HttpStatus

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AbstractController {

    def contexts = []
    protected boolean contextIsSupported(String context){
        return  ['dev','test','preprod','prod'].contains(context)
    }

    protected badContextRequest(HttpServletRequest request, HttpServletResponse response, String context){
        response.setStatus(HttpStatus.BAD_REQUEST.value())
        return ['status':HttpStatus.BAD_REQUEST,'error':"Context (${context}) not supported".toString(),'path':request.servletPath]
    }

    protected errorRequest(HttpServletRequest request, HttpServletResponse response, Exception exception){
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
        println "Exception is -> "+org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exception)
        return ['status':HttpStatus.INTERNAL_SERVER_ERROR,'error':org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(exception),'path':request.servletPath]
    }

}
