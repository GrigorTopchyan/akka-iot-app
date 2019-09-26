package edu.training.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;;
import java.util.Optional;

/**
 * Created by grigort on 9/13/2019.
 */
public class IotSupervisor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(IotSupervisor.class,IotSupervisor::new);
    }

    @Override
    public void preStart() throws Exception {
        log.info("Iot Application started");
    }

    @Override
    public void postStop() throws Exception {
        log.info("Iot Application stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }

    public static final class ReadTemperatue{
        final long requestId;


        public ReadTemperatue(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class RespondTemperature {
        final Optional<Double> value;
        final long requestId;

        public RespondTemperature(Optional<Double> value, long requestId) {
            this.value = value;
            this.requestId = requestId;
        }
    }
}
