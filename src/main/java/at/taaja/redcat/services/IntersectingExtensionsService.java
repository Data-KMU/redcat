package at.taaja.redcat.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.quarkus.runtime.StartupEvent;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.models.views.SpatialRecordView;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.BadRequestException;


@ApplicationScoped
@JBossLog
public class IntersectingExtensionsService {

    @ConfigProperty(name = "purple-tiger.url")
    private String purpleTiger;

    private ObjectWriter objectWriter;

    private HttpClient client;
    private RequestConfig requestConfig;
    private ObjectReader objectReader;

    void onStart(@Observes StartupEvent ev) {
        this.objectWriter = new ObjectMapper().writerWithView(SpatialRecordView.Coordinates.class);
        this.objectReader = new ObjectMapper().readerFor(LocationInformation.class);
        this.client = HttpClientBuilder.create().build();

        // 3 sec timeout
//        this.requestConfig = RequestConfig.custom().setConnectTimeout(3000).build();
    }

    @SneakyThrows
    public LocationInformation calculate(SpatialEntity spatialEntity){

        HttpPost httpPost = new HttpPost(this.purpleTiger);

        httpPost.setEntity(new ByteArrayEntity(
                objectWriter.writeValueAsBytes(spatialEntity)
        ));
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setConfig(this.requestConfig);
        HttpResponse response = this.client.execute(httpPost);
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new BadRequestException("cant resolve coordinates");
        }

        LocationInformation locationInformation = objectReader.readValue(
                response.getEntity().getContent()
        );
        locationInformation.setOriginator(spatialEntity);

        return locationInformation;
    }

}
