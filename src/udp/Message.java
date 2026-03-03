package udp;

import java.net.DatagramPacket;
import java.util.HashMap;

public class Message {
    public MessageType type;
    private HashMap<String, String> data = new HashMap<>();

    public Message(MessageType type) {
        this.type = type;
    }

    public void setData(String key, Object value) {
        data.put(key, value.toString());
    }

    public String getData(String key) {
        return data.get(key);
    }

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
