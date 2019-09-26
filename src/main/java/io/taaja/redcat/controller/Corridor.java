package io.taaja.redcat.controller;

import io.taaja.redcat.config.Constants;
import io.taaja.redcat.model.CorridorModel;
import io.taaja.redcat.repository.CorridorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Constants.API_PREFIX + "/corridor")
public class Corridor {

    @Autowired
    private CorridorRepository corridorRepository;

    @GetMapping("/add")
    public Mono<CorridorModel> add(){
        CorridorModel c = new CorridorModel();
        c.setTitle("test");
        return this.corridorRepository.insert(c);
    }

    @GetMapping("/get")
    public Flux<CorridorModel> get(){
        return this.corridorRepository.findTopByCreatedDate();
    }


    @GetMapping("/all")
    public Flux<CorridorModel> all(){
        return this.corridorRepository.findAll();
    }

    @GetMapping("/clearAll")
    public Mono<Void> clearAll(){
        return this.corridorRepository.deleteAll();
    }

    @GetMapping("/update/{corridorId}")
    public Mono<CorridorModel> update(
            @PathVariable String corridorId
    ){
        CorridorModel c = new CorridorModel();
        c.setTitle("updated");
        c.setId(corridorId);
        return this.corridorRepository.insert(c);
    }


//    @GetMapping("/all")
//    public String getFullTwin(){
//        return "all corridor";
//    }

    @GetMapping("/detail/{corridorId}")
    public String getCorridorTwin(
            @PathVariable String corridorId
    ){
        return "corridor : " + corridorId;
    }

}