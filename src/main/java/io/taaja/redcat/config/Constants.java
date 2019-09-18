package io.taaja.redcat.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class Constants {

    public static final String API_PREFIX = "/v1";

    public static final ResponseEntity RESPONSE_OK = ResponseEntity.ok().build();

    public static final ResponseEntity RESPONSE_NOT_FOUND = ResponseEntity.notFound().build();

    public static final ResponseEntity RESPONSE_BAD_REQUEST = ResponseEntity.badRequest().build();

    public static final ResponseEntity RESPONSE_UNPROCESSABLE_ENTITY = ResponseEntity.unprocessableEntity().build();

    public static HttpHeaders NO_CACHE_HEADER;
    static {
        Constants.NO_CACHE_HEADER =  new HttpHeaders();
        Constants.NO_CACHE_HEADER.add("Cache-Control", "no-cache, no-store, must-revalidate");
        Constants.NO_CACHE_HEADER.add("Pragma", "no-cache");
        Constants.NO_CACHE_HEADER.add("Expires", "0");
    }



}
