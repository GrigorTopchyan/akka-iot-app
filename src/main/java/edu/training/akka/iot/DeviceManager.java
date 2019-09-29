package edu.training.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
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


    private void onTrackDevice(RequrstTrackDevice trackMsg){
        ActorRef deviceGroup = groupIdToActor.get(trackMsg.groupId);
        if (deviceGroup != null){
            deviceGroup.forward(trackMsg,getContext());
        }else {
            log.info("Creating device group for {} ",trackMsg.groupId);
            ActorRef actorGroup = getContext().actorOf(DeviceGroup.props(trackMsg.groupId),"group-"+ trackMsg.groupId);
            getContext().watch(actorGroup);
            actorGroup.forward(trackMsg,getContext());
            groupIdToActor.put(trackMsg.groupId,actorGroup);
            actorToGroupId.put(actorGroup,trackMsg.groupId);
        }
    }


    private void onTerminated(Terminated t){
        ActorRef actorGroup = t.actor();
        String groupId = actorToGroupId.get(actorGroup);
        log.info("Actor group for {} has been terminateed ", groupId);
        groupIdToActor.remove(groupId);
        actorToGroupId.remove(actorGroup);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequrstTrackDevice.class, this::onTrackDevice)
                .match(Terminated.class,this::onTerminated)
                .build();
    }
}
