package edu.training.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by grigort on 9/26/2019.
 */
public class DeviceGroup extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    final String groupId;

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props(String groupId) {
        return Props.create(DeviceGroup.class, () -> new DeviceGroup(groupId));
    }

    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToDeviceId = new HashMap<>();


    public static final class RequestDeviceList {
        private final long requestId;

        public RequestDeviceList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyDeviceList {
        final Set<String> deviceIds;
        final long requestId;

        public ReplyDeviceList(long requestId, Set<String> deviceIds) {
            this.deviceIds = deviceIds;
            this.requestId = requestId;
        }


    }

    @Override
    public void preStart() throws Exception {
        log.info("DeveceGroup {} started", groupId);
    }

    @Override
    public void postStop() throws Exception {
        log.info("DeviceGroup {} stopped", groupId);
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }


    private void onTrackDevice(DeviceManager.RequrstTrackDevice trackMsg) {
        if (this.groupId.equals(trackMsg.groupId)) {
            ActorRef deviceActor = deviceIdToActor.get(trackMsg.deviceId);
            if (deviceActor != null) {
                deviceActor.forward(trackMsg, getContext());
            } else {
                log.info("Creating device actor for {}", trackMsg.deviceId);
                deviceActor = getContext().actorOf(Device.props(groupId, trackMsg.deviceId), "device-" + trackMsg.deviceId);
                getContext().watch(deviceActor);
                actorToDeviceId.put(deviceActor, trackMsg.deviceId);
                deviceIdToActor.put(trackMsg.deviceId, deviceActor);
                deviceActor.forward(trackMsg, getContext());
            }
        } else {
            log.warning("Ignoring TrackDevece request for {}. This actor is responsible for {}", groupId, this.groupId);
        }
    }

    private void onTerminated(Terminated t) {
        ActorRef deviceActor = t.actor();
        String deviceId = actorToDeviceId.get(deviceActor);
        log.info("Devce actor for {} has been terminated ", deviceId);
        actorToDeviceId.remove(deviceActor);
        deviceIdToActor.remove(deviceId);

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequrstTrackDevice.class, this::onTrackDevice)
                .match(Terminated.class, this::onTerminated)
                .match(RequestDeviceList.class, this::onDeviceList)
                .build();
    }
}
