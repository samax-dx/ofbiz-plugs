package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
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
                    "spPlaceSmsPackageOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "request", request,
                            "response", response
                    ))
            );

            if (packageOrder.get("errorMessage") != null) {
                return packageOrder;
            }

            Map<String, Object> creditedBalance = dispatcher.runSync(
                    "spAddPartyProductBalanceForOrder",
                    ServiceContextUtil.authorizeContext(UtilMisc.toMap(
                            "partyId", packageOrder.get("partyId"),
                            "productId", packageOrder.get("productId"),
                            "orderId", packageOrder.get("orderId")
                    ))
            );

            if (packageOrder.get("errorMessage") != null) {
                return creditedBalance;
            }

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

//    @Authorize
//    @CrossOrigin(origins = "*")
//    @RequestMapping(
//            value = "/listSmsPackages",
//            method = RequestMethod.POST,
//            consumes = {"application/json"},
//            produces = {"application/json"}
//    )
//    public Object listSmsPackages(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException {
//        Map<String, Object> result = QueryUtil.find(dispatcher, "ProductPriceType", payload);
//        return UtilMisc.toMap("payments", result.get("list"), "count", result.get("listSize"));
//    }
}
