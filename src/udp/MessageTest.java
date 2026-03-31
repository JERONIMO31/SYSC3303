package udp;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testConstructor_setsTypeCorrectly() {
        Message msg = new Message(MessageType.INIT);
        assertEquals(MessageType.INIT, msg.type);
    }

    @Test
    void testSetAndGetData() {
        Message msg = new Message(MessageType.ASSIGNMENT);

        msg.setData("droneId", 5);
        msg.setData("location", "10,20");

        assertEquals("5", msg.getData("droneId"));
        assertEquals("10,20", msg.getData("location"));
    }

    @Test
    void testGetData_missingKey_returnsNull() {
        Message msg = new Message(MessageType.NEW_INCIDENT);
        assertNull(msg.getData("missing"));
    }

    @Test
    void testToDatagramPacket_format() {
        Message msg = new Message(MessageType.AGENT_DEPLOYED);
        msg.setData("fire", "A");
        msg.setData("amount", 10);

        DatagramPacket packet = msg.toDatagramPacket();
        String payload = new String(packet.getData(), 0, packet.getLength());

        assertTrue(payload.startsWith("AGENT_DEPLOYED:::"));

        assertTrue(payload.contains("fire===A"));
        assertTrue(payload.contains("amount===10"));

        assertTrue(payload.contains("---"));
    }

    @Test
    void testToDatagramPacket_emptyData() {
        Message msg = new Message(MessageType.DRONE_STATUS);

        DatagramPacket packet = msg.toDatagramPacket();
        String payload = new String(packet.getData(), 0, packet.getLength());

        assertEquals("DRONE_STATUS:::", payload);
    }

    @Test
    void testFromDatagramPacket_basic() {
        String raw = "INIT:::key===value";
        DatagramPacket packet = new DatagramPacket(raw.getBytes(), raw.length());

        Message msg = Message.fromDatagramPacket(packet);

        assertEquals(MessageType.INIT, msg.type);
        assertEquals("value", msg.getData("key"));
    }

    @Test
    void testFromDatagramPacket_multiplePairs() {
        String raw = "ASSIGNMENT:::drone===1---fire===A";
        DatagramPacket packet = new DatagramPacket(raw.getBytes(), raw.length());

        Message msg = Message.fromDatagramPacket(packet);

        assertEquals("1", msg.getData("drone"));
        assertEquals("A", msg.getData("fire"));
    }

    @Test
    void testRoundTrip_serializationDeserialization() {
        Message original = new Message(MessageType.FIRE_EXTINGUISHED);
        original.setData("fireId", "X1");
        original.setData("time", "12:00");

        DatagramPacket packet = original.toDatagramPacket();
        Message parsed = Message.fromDatagramPacket(packet);

        assertEquals(MessageType.FIRE_EXTINGUISHED, parsed.type);
        assertEquals("X1", parsed.getData("fireId"));
        assertEquals("12:00", parsed.getData("time"));
    }

    @Test
    void testSetData_overwritesValue() {
        Message msg = new Message(MessageType.DRONE_FAULT);

        msg.setData("drone", 1);
        msg.setData("drone", 2);

        assertEquals("2", msg.getData("drone"));
    }

    @Test
    void testNumericValues_storedAsString() {
        Message msg = new Message(MessageType.AGENT_DEPLOYED);

        msg.setData("amount", 50);

        assertEquals("50", msg.getData("amount"));
    }

    @Test
    void testFromDatagramPacket_preservesAllKeys() {
        String raw = "DRONE_STATUS:::id===7---status===OK---battery===90";
        DatagramPacket packet = new DatagramPacket(raw.getBytes(), raw.length());

        Message msg = Message.fromDatagramPacket(packet);

        assertEquals("7", msg.getData("id"));
        assertEquals("OK", msg.getData("status"));
        assertEquals("90", msg.getData("battery"));
    }
}