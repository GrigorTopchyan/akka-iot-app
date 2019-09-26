package edu.training.akka.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestKit;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * Created by grigort on 9/25/2019.
 */
public class IotAppTest {
    @Test
    public void testReplyWithEmptyReadingIfNoTeemperatureIsKnown(){
        ActorSystem system = ActorSystem.create("iot-system");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group","device"));
        deviceActor.tell(new Device.ReadTemperature(42L),probe.testActor());
        Device.RespondTemperature response = probe.expectMsgClass(Device.RespondTemperature.class);
        Assert.assertEquals(42L,response.requestId);
        Assert.assertEquals(Optional.empty(),response.value);
    }

    @Test
    public void testReplyWithLatestTemperatureReading(){
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group","device"));
        deviceActor.tell(new Device.RecordTemperature(1L,24.0),probe.testActor());
        Assert.assertEquals(1L,probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new Device.ReadTemperature(2L),probe.testActor());
        Device.RespondTemperature response = probe.expectMsgClass(Device.RespondTemperature.class);
        Assert.assertEquals(2L,response.requestId);
        Assert.assertEquals(Optional.of(24.0),response.value);

        deviceActor.tell(new Device.RecordTemperature(3L,55.0),probe.testActor());
        Assert.assertEquals(3L,probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
        deviceActor.tell(new Device.ReadTemperature(4L),probe.testActor());
        Device.RespondTemperature response2 = probe.expectMsgClass(Device.RespondTemperature.class);

        Assert.assertEquals(4L,response2.requestId);
        Assert.assertEquals(Optional.of(55.0),response2.value);

    }
}
