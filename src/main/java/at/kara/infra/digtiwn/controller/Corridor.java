package at.kara.infra.digtiwn.controller;

import at.kara.infra.digtiwn.config.Constants;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/corridor")
public class Corridor {


    @GetMapping("/all")
    public String getFullTwin(){
        return "all corridor";
    }

    @GetMapping("/detail/{corridorId}")
    public String getCorridorTwin(
            @PathVariable String corridorId
    ){
        return "corridor : " + corridorId;
    }

}