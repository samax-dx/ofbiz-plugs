package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import OfbizSpring.Util.ServiceContextUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
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

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createOrder(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> productOrder = dispatcher.runSync(
                    "spCreateOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "request", request,
                            "response", response
                    ))
            );

            if (productOrder.get("errorMessage") != null) {
                return productOrder;
            }

            Map<String, Object> creditedBalance = dispatcher.runSync(
                    "spAddPartyProductBalanceForOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "partyId", productOrder.get("partyId"),
                            "productId", productOrder.get("productId"),
                            "orderId", productOrder.get("orderId")
                    ))
            );

            if (productOrder.get("errorMessage") != null) {
                return creditedBalance;
            }

//            return ServiceUtil.returnSuccess(String.format(
//                    "Order ID: %s, Credited Unit: %s, Billed Amount: %s",
//                    productOrder.get("orderId"),
//                    creditedBalance.get("amount"),
//                    productOrder.get("totalPrice")
//            ));

            return productOrder;
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createPartyOrder",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object createPartyOrder(HttpServletRequest request, HttpServletResponse response, @RequestAttribute Map<String, String> signedParty) {
        try {
            Map<String, Object> productOrder = dispatcher.runSync(
                    "spCreateOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "request", request,
                            "response", response,
                            "partyId", signedParty.get("partyId")
                    ))
            );

            if (productOrder.get("errorMessage") != null) {
                return productOrder;
            }

            Map<String, Object> creditedBalance = dispatcher.runSync(
                    "spAddPartyProductBalanceForOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "partyId", productOrder.get("partyId"),
                            "productId", productOrder.get("productId"),
                            "orderId", productOrder.get("orderId")
                    ))
            );

            if (productOrder.get("errorMessage") != null) {
                return creditedBalance;
            }

//            return ServiceUtil.returnSuccess(String.format(
//                    "Order ID: %s, Credited Unit: %s, Billed Amount: %s",
//                    productOrder.get("orderId"),
//                    creditedBalance.get("amount"),
//                    productOrder.get("totalPrice")
//            ));

            return productOrder;
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listOrders",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listOrders(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "OrderLookupView", payload);
        return UtilMisc.toMap("orders", result.get("list"), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPartyOrders",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPartyOrders(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
        payload.put("party.partyId", signedParty.get("partyId"));

        Map<String, Object> result = QueryUtil.find(dispatcher, "OrderLookupView", payload);
        return UtilMisc.toMap("orders", result.get("list"), "count", result.get("listSize"));
    }
}
