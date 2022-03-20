package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.HttpUtil;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/Order")
public class Order {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createOrder(HttpServletRequest request, HttpServletResponse response) {
        try {
            return dispatcher.runSync("spCreateOrder", UtilMisc.toMap(
                    "request", request, "response", response, "login.username", "admin", "login.password", "ofbiz"
            ));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createSmsPackageOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createSmsPackageOrder(HttpServletRequest request, HttpServletResponse response) {
        try {
            return dispatcher.runSync("spCreateSmsPackageOrder", UtilMisc.toMap(
                    "request", request, "response", response, "login.username", "admin", "login.password", "ofbiz"
            ));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createSmsUnitOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createSmsUnitOrder(HttpServletRequest request, HttpServletResponse response) {
        try {
            return dispatcher.runSync("spCreateSmsUnitOrder", UtilMisc.toMap(
                    "request", request, "response", response, "login.username", "admin", "login.password", "ofbiz"
            ));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/purchaseSmsPackage",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object purchaseSmsPackage(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> packageOrder = dispatcher.runSync(
                    "spCreateSmsPackageOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "request", request,
                            "response", response
                    ))
            );

            Map<String, Object> creditedBalance = dispatcher.runSync(
                    "spAddPartyProductBalanceForOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "partyId", packageOrder.get("partyId"),
                            "productId", packageOrder.get("productId"),
                            "orderId", packageOrder.get("orderId")
                    ))
            );

            return ServiceUtil.returnSuccess(String.format(
                    "Order ID: %s, Credited Unit: %s, Billed Amount: %s",
                    packageOrder.get("orderId"),
                    creditedBalance.get("amount"),
                    packageOrder.get("totalPrice")
            ));
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }
}
