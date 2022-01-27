package OfbizSpring.Util;

import java.util.Map;

import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityListIterator;

public class MapUtil {
    static Map<String, Object> remapIt(Map<String, Object> data) throws GenericEntityException {
        if (data.get("listIt") instanceof EntityListIterator) {
            EntityListIterator i = (EntityListIterator) data.get("listIt");
            data.put("listIt", i.getCompleteList());
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> remap(Object data) {
        //noinspection unchecked
        return (Map<String, Object>) data;
    }
}
