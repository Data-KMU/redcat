package at.kara.infra.digtiwn.controller;

import at.kara.infra.digtiwn.config.Constants;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/area")
public class Area {


    @GetMapping("")
    public String getFullTwin(){
        return "full";
    }


}