package edu.training.akka.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

/**
 * Created by grigort on 9/13/2019.
 */
public class IotApp {

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create("iot-system");

        try{
            ActorRef superviser = system.actorOf(IotSupervisor.props(),"iot-supervisor");
            System.out.println("Press ENTER to exit the system");
            System.in.read();
        }finally {
            system.terminate();
        }
    }
}
