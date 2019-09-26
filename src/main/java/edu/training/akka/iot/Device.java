package edu.training.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Optional;

/**
 * Created by grigort on 9/13/2019.
 */
public class Device extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    final String groupId;

    final String deviceId;


    public Device(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    public static Props props(String groupId, String deviceId) {
        return Props.create(Device.class, () -> new Device(groupId, deviceId));
    }

    public static final class ReadTemperature {
        final long requestId;

        public ReadTemperature(long requestId) {
            this.requestId = requestId;
        }
    }

    public static class RespondTemperature {
        final long requestId;
        final Optional<Double> value;

        public RespondTemperature(long requestId, Optional<Double> value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    public static class RecordTemperature {
        final double value;
        final long requestId;


        public RecordTemperature(long requestId,double value) {
            this.value = value;
            this.requestId = requestId;
        }
    }

    public static final class TemperatureRecorded {
        final long requestId;


        public TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }
    }

    Optional<Double> lastTemperatureReading = Optional.empty();


    @Override
    public void preStart() throws Exception {
        log.info("Device actor {}-{} started", groupId, deviceId);
    }

    @Override
    public void postStop() throws Exception {
        log.info("Device actor {}-{} stopped", groupId, deviceId);

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        ReadTemperature.class, r -> {
                            getSender().tell(new RespondTemperature(r.requestId, lastTemperatureReading), getSelf());
                        })
                .match(
                        RecordTemperature.class, r -> {
                            log.info("Record temperature reading {} with {}", r.value, r.requestId);
                            lastTemperatureReading = Optional.of(r.value);
                            getSender().tell(new TemperatureRecorded(r.requestId), getSelf());
                        })
                .build();
    }
}
