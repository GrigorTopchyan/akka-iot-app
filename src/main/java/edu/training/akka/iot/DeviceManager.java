package edu.training.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by grigort on 9/26/2019.
 */
public class DeviceManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(),this);

    public static Props props(){
        return Props.create(DeviceManager::new);
    }

    public static final class RequrstTrackDevice {
        public final String groupId;
        public final String deviceId;

        public RequrstTrackDevice(String groupId, String deviceId) {
            this.groupId = groupId;
            this.deviceId = deviceId;
        }
    }

    public static final class DeviceRegistered {}

    final Map<String,ActorRef> groupIdToActor = new HashMap<>();
    final Map<ActorRef,String> actorToGroupId = new HashMap<>();

    @Override
    public void preStart() throws Exception {
        log.info("DeviceManager started");
    }

    @Override
    public void postStop() throws Exception {
        log.info("DeveceManager stopped");
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
