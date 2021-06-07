package at.taaja.redcat.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.taaja.models.generic.LocationInformation;
import io.taaja.models.record.spatial.SpatialEntity;
import io.taaja.services.AbstractIntersectingExtensionsService;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashMap;

@ApplicationScoped
@JBossLog
public class IntersectingExtensionsService extends AbstractIntersectingExtensionsService {

    ObjectMapper objectMapper = new ObjectMapper();
    @Override
    @SneakyThrows
    public LocationInformation calculate(SpatialEntity spatialEntity) {
        HttpPost httpPost = new HttpPost(this.url + "/v1/calculate/intersectingExtensions");
        httpPost.setEntity(new ByteArrayEntity(this.objectWriter.writeValueAsBytes(spatialEntity)));
        httpPost.setHeader("Content-type", "application/json");
        if (this.requestConfig != null) {
            httpPost.setConfig(this.requestConfig);
        }

        HttpResponse response = this.client.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new BadRequestException("cant resolve coordinates");
        } else {
            LinkedHashMap<String, Object> input = objectMapper.readValue(response.getEntity().getContent(), LinkedHashMap.class);
            input.remove("elevation");
            LocationInformation locationInformation = objectMapper.convertValue(input, LocationInformation.class);
//            LocationInformation locationInformation = this.objectReader.readValue(response.getEntity().getContent());
            locationInformation.setOriginator(spatialEntity);
            return locationInformation;
        }

    }

    @SneakyThrows
    public LocationInformation calculate(@QueryParam("longitude") float longitude, @QueryParam("latitude") float latitude, @QueryParam("altitude") Float altitude) {
        String var10002 = this.url;
        HttpGet httpGet = new HttpGet(var10002 + "/v1/encode/position?longitude=" + longitude + "&latitude=" + latitude + (altitude == null ? "" : "&altitude=" + altitude));
        if (this.requestConfig != null) {
            httpGet.setConfig(this.requestConfig);
        }

        HttpResponse response = this.client.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new BadRequestException("cant resolve coordinates");
        } else {

            LinkedHashMap<String, Object> input = objectMapper.readValue(response.getEntity().getContent(), LinkedHashMap.class);
            input.remove("elevation");
            return  (LocationInformation) objectMapper.convertValue(input, LocationInformation.class);

//            LocationInformation locationInformation = this.objectReader.readValue(response.getEntity().getContent());
//            return (LocationInformation)this.objectReader.readValue(response.getEntity().getContent());
        }
    }

}
