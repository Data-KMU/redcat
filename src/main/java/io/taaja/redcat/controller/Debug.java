package io.taaja.redcat.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnExpression("${app.enableDebugController}")
@RequestMapping("/debug")
public class Debug {


    @GetMapping("/ping")
    public Object ping(){
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/fail")
    public Object fail(){
        return ResponseEntity.badRequest().build();
    }


}