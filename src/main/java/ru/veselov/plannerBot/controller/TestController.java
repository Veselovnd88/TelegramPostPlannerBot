package ru.veselov.plannerBot.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {


    @GetMapping
    public String getMethod(){
        return "PlannerBot";
    }

}
