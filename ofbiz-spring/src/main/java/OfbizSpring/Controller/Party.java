package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Annotations.OfbizService;
import OfbizSpring.Util.JwtHelper;
import OfbizSpring.Util.MapUtil;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/Party")
public class Party {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/login",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object login(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> rqUser = new HashMap<>(payload);
        Map<String, Object> dbUser;

        try {
            dbUser = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", rqUser.get("username")).queryFirst();
            if (dbUser == null) {
                throw new Exception("User not found");
            }
        } catch (GenericEntityException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        boolean isAuthentic = LoginServices.checkPassword(
                (String) dbUser.get("currentPassword"),
                Boolean.TRUE,
                (String) rqUser.getOrDefault("password", "")
        );

        if (isAuthentic) {
            Map<String, String> responsePayload = new HashMap<>();
            responsePayload.put("partyId", (String) dbUser.get("partyId"));
            responsePayload.put("loginId", (String) dbUser.get("userLoginId"));
            return UtilMisc.toMap("token", new JwtHelper().getUserToken(responsePayload));
        } else {
            return ServiceUtil.returnError("Invalid password");
        }
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createParty",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createParty(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        try {
            Map<String, Object> result = dispatcher.runSync("spCreateParty", ServiceContextUtil.authorizeContext(payload));

            if (result.get("partyId") == null) {
                throw new Exception((String) result.getOrDefault("errorMessage", "Unknown result"));
            } else {
                return UtilMisc.toMap("id", result.get("partyId"));
            }
        } catch (Exception ex) {
            return UtilMisc.toMap("errorMessage", ex.getMessage());
        }
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/findParty",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object findParty(@RequestBody Map<String, Object> request, HttpServletResponse response, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        return EntityQuery.use(delegator).from("PartyPortalView").where("id", signedParty.get("partyId")).queryFirst();
    }

    @Authorize(groups = { "FULLADMIN" })
    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/findParties",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object findParties(@RequestBody Map<String, Object> request, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = MapUtil.remapIt(dispatcher.runSync("performFind", UtilMisc.toMap(
                "entityName", "PartyPortalView",
                "inputFields", request
        )));
        return result.get("listIt");
    }
}
