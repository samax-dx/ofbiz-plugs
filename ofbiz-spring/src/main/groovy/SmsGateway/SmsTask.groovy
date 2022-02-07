package SmsGateway;

import java.util.Map;
import java.util.HashMap;

public class SmsTask {
    public String CampaignName;
    public String SenderId;
    public String MobileNumbers;
    public String Message;
    public boolean Is_Unicode = true;
    public boolean Is_Flash = false;
    public Map<String, Object> toMap()
    {
        return new HashMap<String, Object>() {{
            put("CampaignName", CampaignName);
            put("SenderId", SenderId);
            put("MobileNumbers", MobileNumbers);
            put("Message", Message);
            put("Is_Unicode", Is_Unicode);
            put("Is_Flash", Is_Flash);
        }};
    }
}
