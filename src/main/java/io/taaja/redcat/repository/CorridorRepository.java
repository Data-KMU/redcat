package io.taaja.redcat.repository;


import io.taaja.redcat.model.CorridorModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * https://medium.com/@beladiyahardik7/spring-boot-2-mongodb-reactive-programming-b20a9a5bd6c
 */
@Repository
public interface CorridorRepository extends ReactiveMongoRepository<CorridorModel, String> {

    Flux<CorridorModel> findByTitle(String title);

    Flux<CorridorModel> findTopByCreatedDate();

}
