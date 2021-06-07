#!/bin/bash
./mvnw compile quarkus:dev -Ddebug=5010 -Dquarkus.http.host=0.0.0.0
