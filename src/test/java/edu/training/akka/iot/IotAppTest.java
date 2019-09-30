package edu.training.akka.iot;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.TestKit;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by grigort on 9/25/2019.
 */
public class IotAppTest {
    @Test
    public void testReplyWithEmptyReadingIfNoTeemperatureIsKnown() {
        ActorSystem system = ActorSystem.create("iot-system");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
        deviceActor.tell(new Device.ReadTemperature(42L), probe.testActor());
        Device.RespondTemperature response = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(42L, response.requestId);
        assertEquals(Optional.empty(), response.value);
    }

    @Test
    public void testReplyWithLatestTemperatureReading() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
        deviceActor.tell(new Device.RecordTemperature(1L, 24.0), probe.testActor());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new Device.ReadTemperature(2L), probe.testActor());
        Device.RespondTemperature response = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(2L, response.requestId);
        assertEquals(Optional.of(24.0), response.value);

        deviceActor.tell(new Device.RecordTemperature(3L, 55.0), probe.testActor());
        assertEquals(3L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
        deviceActor.tell(new Device.ReadTemperature(4L), probe.testActor());
        Device.RespondTemperature response2 = probe.expectMsgClass(Device.RespondTemperature.class);

        assertEquals(4L, response2.requestId);
        assertEquals(Optional.of(55.0), response2.value);

    }

    @Test
    public void testReplyToRegistrationRequest() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
        deviceActor.tell(new DeviceManager.RequrstTrackDevice("group", "device"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        assertEquals(deviceActor, probe.lastSender());
    }

    @Test
    public void testIgnoreWrongRegistrationRequests() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new DeviceManager.RequrstTrackDevice("wrongGroup", "device"), probe.testActor());
        probe.expectNoMessage();

        deviceActor.tell(new DeviceManager.RequrstTrackDevice("group", "wrongDevice"), probe.testActor());
        probe.expectNoMessage();
    }

    @Test
    public void testRegisterDeviceActor() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"));
        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device1"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceActor1 = probe.lastSender();

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device2"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceActor2 = probe.lastSender();

        Assert.assertNotEquals(deviceActor1, deviceActor2);

        deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), probe.testActor());
        assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);


        deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), probe.testActor());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
    }


    @Test
    public void testIgnoreRequestsForWrongGroupId() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"));

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("wrongGroup", "device1"), probe.testActor());
        probe.expectNoMessage();
    }

    @Test
    public void testReturnSameActorForSameDeviceId() {
        ActorSystem system = ActorSystem.create("iot-system-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"));

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef device1 = probe.lastSender();

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef device2 = probe.lastSender();

        Assert.assertEquals(device1, device2);
    }

    @Test
    public void testListActiveDevices() {
        ActorSystem system = ActorSystem.create("iot-app-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"));

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device1"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);


        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device2"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);

        deviceGroup.tell(new DeviceGroup.RequestDeviceList(0L), probe.testActor());
        DeviceGroup.ReplyDeviceList deviceList = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
        Assert.assertEquals(Set.of("device1", "device2"), deviceList.deviceIds);
    }

    @Test
    public void testListActiveDevicesAfterOneShutdown() {
        ActorSystem system = ActorSystem.create("iot-app-test");
        TestKit probe = new TestKit(system);
        ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"));

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device1"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef toBeTerminated = probe.lastSender();

        deviceGroup.tell(new DeviceManager.RequrstTrackDevice("group", "device2"), probe.testActor());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);

        deviceGroup.tell(new DeviceGroup.RequestDeviceList(0L), probe.testActor());
        DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(Set.of("device1", "device2"), reply.deviceIds);

        probe.watch(toBeTerminated);
        toBeTerminated.tell(PoisonPill.getInstance(), ActorRef.noSender());
        probe.expectTerminated(toBeTerminated, Duration.create(3L, TimeUnit.SECONDS));

        deviceGroup.tell(new DeviceGroup.RequestDeviceList(1L), probe.testActor());
        DeviceGroup.ReplyDeviceList list = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
        Assert.assertEquals(Set.of("device2"), list.deviceIds);
    }




}
