package io.taaja.redcat.controller;

import io.taaja.redcat.config.Constants;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/area")
public class Area {


    @GetMapping("")
    public String getFullTwin(){
        return "full";
    }


}