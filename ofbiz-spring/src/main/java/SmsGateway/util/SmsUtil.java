package SmsGateway.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class SmsUtil {
    public static int smsCount(String message) {
        return (int) Math.ceil((double) message.getBytes(StandardCharsets.UTF_8).length / 140);
    }

    public static List<String> parseContacts(String contacts) {
        return Arrays.asList(contacts.split("\\s*,\\s*"));
    }

    public static int contactCount(String contacts) {
        return parseContacts(contacts).size();
    }
}
