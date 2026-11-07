package me.kitkas1412.tixora.controller.resource;

import me.kitkas1412.tixora.application.service.event.EventAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HiController {
    private final EventAppService eventAppService;

    public HiController(EventAppService eventAppService) {
        this.eventAppService = eventAppService;
    }

    @GetMapping("/hi")
    public String hello(){
        return eventAppService.sayHi("Hi");
    }

}
