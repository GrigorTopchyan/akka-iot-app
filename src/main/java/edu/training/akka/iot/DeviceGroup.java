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
public class DeviceGroup extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(),this);

    final String groupId;

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props (String groupId){
        return Props.create(DeviceGroup.class,() -> new DeviceGroup(groupId));
    }

    final Map<String,ActorRef> deviceIdToActor = new HashMap<>();

    @Override
    public void preStart() throws Exception {
        log.info("DeveceGroup {} started",groupId);
    }

    @Override
    public void postStop() throws Exception {
        log.info("DeviceGroup {} stopped", groupId);
    }


    private void onTrackDevice(DeviceManager.RequrstTrackDevice trackMsg){
        if (this.groupId.equals(trackMsg.groupId)){
            ActorRef deviceActor = deviceIdToActor.get(trackMsg.deviceId);
            if (deviceActor != null){
                deviceActor.forward(trackMsg,getContext());
            }else {
                log.info("Creating device actor for {}",trackMsg.deviceId);
                deviceActor = getContext().actorOf(Device.props(groupId,trackMsg.deviceId),"device-" + trackMsg.deviceId);
                deviceIdToActor.put(trackMsg.deviceId,deviceActor);
                deviceActor.forward(trackMsg,getContext());
            }
        }else {
            log.warning("Ignoring TrackDevece request for {}. This actor is responsible for {}",groupId,this.groupId);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(
                DeviceManager.RequrstTrackDevice.class,this::onTrackDevice
        ).build();
    }
}
