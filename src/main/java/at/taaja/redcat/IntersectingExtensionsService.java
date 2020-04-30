package at.taaja.redcat;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.record.spatial.SpatialEntity;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.NotAllowedException;


@ApplicationScoped
@JBossLog
public class IntersectingExtensionsService {

    @ConfigProperty(name = "purple-tiger.url")
    private String purpleTiger;

    private ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient client;

    void onStart(@Observes StartupEvent ev) {
        this.client = HttpClientBuilder.create().build();
    }

    @SneakyThrows
    public LocationInformation calculate(SpatialEntity spatialEntity){

        HttpPost httpPost = new HttpPost(this.purpleTiger);

        httpPost.setEntity(new ByteArrayEntity(
                objectMapper.writeValueAsBytes(spatialEntity)
        ));
        HttpResponse response = this.client.execute(httpPost);
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NotAllowedException("cant resolve coordinates");
        }

        LocationInformation locationInformation = objectMapper.readValue(
                response.getEntity().getContent(),
                LocationInformation.class
        );

        return locationInformation;
    }

}
