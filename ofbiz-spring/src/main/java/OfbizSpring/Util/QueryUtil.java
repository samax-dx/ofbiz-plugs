package OfbizSpring.Util;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Map;

/*
entityName 		False 	String 	IN 	False
filterByDate 		True 	String 	IN 	False
filterByDateValue 		True 	Timestamp 	IN 	False
inputFields 		False 	java.util.Map 	IN 	False
locale 		True 	java.util.Locale 	INOUT 	True
login.password 		True 	String 	IN 	True
login.username 		True 	String 	IN 	True
noConditionFind 		True 	String 	IN 	False
orderBy 		True 	String 	IN 	False
timeZone 		True 	java.util.TimeZone 	INOUT 	True
userLogin 		True 	org.apache.ofbiz.entity.GenericValue 	INOUT 	True
viewIndex 		True 	Integer 	IN 	False
viewSize 		True 	Integer 	IN 	False
visualTheme 		True 	org.apache.ofbiz.widget.renderer.VisualTheme 	INOUT 	True
 */

public class QueryUtil {
    public static Map<String, Object> find(LocalDispatcher dispatcher, String entityName, Map<String, Object> inputFields) throws GenericServiceException {
        int viewPage = 1;
        try {
            int argViewPage = Integer.parseInt(inputFields.get("page").toString());
            viewPage = argViewPage > 0 ? argViewPage : viewPage;
        } catch (Exception ignored) {}

        int viewSize = 10;
        try {
            int argViewSize = Integer.parseInt(inputFields.get("limit").toString());
            viewSize = argViewSize > 0 ? argViewSize : viewSize;
        } catch (Exception ignored) {}

        Object orderBy = inputFields.get("orderBy");

        inputFields.remove("login.username");
        inputFields.remove("login.password");
        inputFields.remove("userLogin");

        inputFields.put("noConditionFind", "Y");

        Map<String, Object> result = dispatcher.runSync("performFindList", ServiceContextUtil.authorizeContext(
            UtilMisc.toMap(
                "entityName", entityName,
                "inputFields", inputFields,
                "viewSize", viewSize,
                "viewIndex", viewPage - 1,
                "orderBy", orderBy
            )
        ));

        result.remove("userLogin");
        return result;
    }

    public static Map<String, Object> findOne(LocalDispatcher dispatcher, String entityName, Map<String, Object> inputFields) throws GenericServiceException {
        Object orderBy = inputFields.get("orderBy");

        inputFields.remove("login.username");
        inputFields.remove("login.password");
        inputFields.remove("userLogin");

        Map<String, Object> result = dispatcher.runSync("performFindItem", ServiceContextUtil.authorizeContext(
            UtilMisc.toMap(
                "entityName", entityName,
                "inputFields", inputFields,
                "orderBy", orderBy
            )
        ));

        result.remove("userLogin");
        return result;
    }
}
