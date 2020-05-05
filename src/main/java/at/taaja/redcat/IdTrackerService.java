package at.taaja.redcat;

import io.quarkus.runtime.StartupEvent;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.Queue;

@ApplicationScoped
@JBossLog
public class IdTrackerService {

    private Queue<String> processedIds;

    void onStart(@Observes StartupEvent ev) {
        this.processedIds = QueueUtils.synchronizedQueue(new CircularFifoQueue<>(100));
    }

    public void addId(String id){
        this.processedIds.add(id);
    }

    public boolean containsId(String id){
        return this.processedIds.contains(id);
    }

}
