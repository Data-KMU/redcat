package io.taaja.redcat.controller;

import io.taaja.redcat.config.Constants;
import io.taaja.redcat.model.CorridorModel;
import io.taaja.redcat.repository.CorridorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(Constants.API_PREFIX + "/corridor")
public class Corridor {

    @Autowired
    private CorridorRepository corridorRepository;

    @GetMapping("/add")
    public ResponseEntity add(){

        CorridorModel c = new CorridorModel();
        c.setTitle("test");
        this.corridorRepository.insert(c);
        return Constants.RESPONSE_OK;
    }


    @GetMapping("/get")
    public Flux<CorridorModel> get(){
        return this.corridorRepository.findAll();
    }


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