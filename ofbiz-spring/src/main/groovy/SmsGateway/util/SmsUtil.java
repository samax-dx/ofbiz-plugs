package SmsGateway.util;

import java.nio.charset.StandardCharsets;

public class SmsUtil {
    public static int smsCount(String message) {
        return (int) Math.ceil((double) message.getBytes(StandardCharsets.UTF_8).length / 140);
    }

    public static int contactCount(String contacts) {
        return contacts.split("\\s*,\\s*").length;
    }
}
