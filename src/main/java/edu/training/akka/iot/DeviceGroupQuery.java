package edu.training.akka.iot;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceGroupQuery extends AbstractActor {

    public static final class CollectionTimeout {}

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(),this);
    final Map<ActorRef,String> actorToDeveceId;
    final long requestId;
    final ActorRef requester;
    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(Map<ActorRef, String> actorToDeveceId, long requestId, ActorRef requester, FiniteDuration timeout) {
        this.actorToDeveceId = actorToDeveceId;
        this.requestId = requestId;
        this.requester = requester;
        queryTimeoutTimer = getContext()
                .getSystem()
                .scheduler()
                .scheduleOnce(timeout,getSelf(),new CollectionTimeout(),getContext().getDispatcher(),getSelf());
    }

    public static Props props(Map<ActorRef,String> actorToDeveceId, long requestId,ActorRef requester,FiniteDuration timeout){
        return Props.create(DeviceGroupQuery.class,()-> new DeviceGroupQuery(actorToDeveceId,requestId,requester,timeout));
    }

    @Override
    public void preStart() throws Exception {
        for (ActorRef deviceActor : actorToDeveceId.keySet()){
            getContext().watch(deviceActor);
            deviceActor.tell(new Device.ReadTemperature(0L),getSelf());
        }
    }

    @Override
    public void postStop() throws Exception {
        queryTimeoutTimer.cancel();
    }

    public Receive waitingForReplies(Map<String,DeviceGroup.TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting){
        return receiveBuilder()
                .match(Device.RespondTemperature.class, r -> {
                    ActorRef deviceActor = getSender();
                    DeviceGroup.TemperatureReading reading =
                            r.value.map(v -> (DeviceGroup.TemperatureReading)new DeviceGroup.Temperature(v)).orElse(DeviceGroup.TemperatureNotAvailable.INSTANCE);
                        receivedResponse(deviceActor,reading,stillWaiting,repliesSoFar);
                })
                .match(Terminated.class, t -> {receivedResponse(t.getActor(),DeviceGroup.DeviceNotAvailable.INSTANCE,stillWaiting,repliesSoFar);})
                .match(CollectionTimeout.class, t -> {
                    Map<String,DeviceGroup.TemperatureReading> replies = new HashMap<>(repliesSoFar);
                    for (ActorRef deviceActor : stillWaiting){
                        String deviceId = actorToDeveceId.get(deviceActor);
                        replies.put(deviceId,DeviceGroup.DeviceTimedOut.INSTANCE);
                    }
                    requester.tell(new DeviceGroup.RespondAllTemperatures(requestId,replies),getSelf());
                })
                .build();
    }

    public void receivedResponse(ActorRef deviceActor,DeviceGroup.TemperatureReading reading,Set<ActorRef> stillWaiting,Map<String,DeviceGroup.TemperatureReading> repliesSoFar){
        getContext().unwatch(deviceActor);
        String deviceId = actorToDeveceId.get(deviceActor);
        Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
        newStillWaiting.remove(deviceActor);
        Map<String,DeviceGroup.TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
        newRepliesSoFar.put(deviceId,reading);
        if (newStillWaiting.isEmpty()){
            requester.tell(new DeviceGroup.RespondAllTemperatures(requestId,newRepliesSoFar),getSelf());
            getContext().stop(getSelf());
        }else {
            getContext().become(waitingForReplies(newRepliesSoFar,newStillWaiting));
        }
    }


    @Override
    public Receive createReceive() {
        return waitingForReplies(new HashMap<>(),actorToDeveceId.keySet());
    }
}
