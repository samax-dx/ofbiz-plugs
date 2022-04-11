package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Annotations.OfbizService;
import OfbizSpring.Util.JwtHelper;
import OfbizSpring.Util.QueryUtil;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.accounting.payment.BillingAccountWorker;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
                return UtilMisc.toMap("partyId", result.get("partyId"));
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
    public Object findParty(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = QueryUtil.findOne(dispatcher, "PartyProfileView", payload);
        return UtilMisc.toMap("party", result.get("item"), "count", result.get("item") == null ? 0 : 1);
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
    public Object findParties(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "PartyProfileView", payload);
        return UtilMisc.toMap("parties", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/profile",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object profile(@RequestBody Map<String, Object> payload, HttpServletResponse response, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        payload.put("partyId", signedParty.get("partyId"));

        Map<String, Object> result = QueryUtil.findOne(dispatcher, "PartyProfileView", payload);
        return UtilMisc.toMap(
                "profile", result.get("item"),
                "count", result.get("listSize"),
                "balance", getPartyBalance(result.get("item"))
        );
    }

    private String getPartyBalance(Object partyRecord) throws GenericEntityException {
        Map<String, Object> party = UtilMisc.toMap(partyRecord);
        return new DecimalFormat("0.00").format(BillingAccountWorker.getBillingAccountAvailableBalance(delegator, (String) party.get("billingAccountId")).multiply(BigDecimal.valueOf(-1)));
    }
}
