package udp;

import java.net.DatagramPacket;
import java.util.HashMap;

/**
 * Represents a UDP message with a type and key-value data pairs.
 * Messages are serialized using ::: as type/data separator, --- between
 * key-value pairs, and === between keys and values.
 */
public class Message {
    public MessageType type;
    private HashMap<String, String> data = new HashMap<>();

    /**
     * Constructs a new Message with the specified type.
     *
     * @param type The message type
     */
    public Message(MessageType type) {
        this.type = type;
    }

    /**
     * Sets a key-value data pair on this message.
     *
     * @param key   The data key
     * @param value The data value (converted to string)
     */
    public void setData(String key, Object value) {
        data.put(key, value.toString());
    }

    /**
     * Gets the value associated with a data key.
     *
     * @param key The data key to look up
     * @return The value string, or null if the key doesn't exist
     */
    public String getData(String key) {
        return data.get(key);
    }

    /**
     * Deserializes a Message from a received DatagramPacket.
     *
     * @param packet The received UDP packet
     * @return The deserialized Message
     */
    public static Message fromDatagramPacket(DatagramPacket packet) {
        String payload = new String(packet.getData(), 0, packet.getLength());

        MessageType type = MessageType.fromString(payload.split(":::")[0]);
        Message message = new Message(type);
        String data = payload.split(":::")[1];
        String[] keyValuePairs = data.split("---");
        for (String pair : keyValuePairs) {
            String[] kv = pair.split("===");
            message.setData(kv[0], kv[1]);
        }
        return message;
    }

    /**
     * Serializes this Message into a DatagramPacket for sending.
     *
     * @return The serialized DatagramPacket
     */
    public DatagramPacket toDatagramPacket() {
        StringBuilder payloadBuilder = new StringBuilder();
        payloadBuilder.append(type.name()).append(":::");

        boolean first = true;
        for (HashMap.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                payloadBuilder.append("---");
            }
            payloadBuilder.append(entry.getKey()).append("===").append(entry.getValue());
            first = false;
        }

        byte[] payload = payloadBuilder.toString().getBytes();
        return new DatagramPacket(payload, payload.length);
    }
}
