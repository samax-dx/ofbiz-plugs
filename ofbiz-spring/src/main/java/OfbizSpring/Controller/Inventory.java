package OfbizSpring.Controller;

import OfbizSpring.Annotations.Authorize;
import OfbizSpring.Util.QueryUtil;
import org.apache.ofbiz.accounting.payment.BillingAccountWorker;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Inventory")
public class Inventory {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    private static List<Map<String, Object>> withStocks(List<?> inventories, Delegator delegator) {
        return inventories.stream()
                .map(inventory -> {
                    if (inventory instanceof GenericValue) {
                        String billingAccountId = ((GenericValue) inventory).get("inventoryId").toString();
                        try {
                            Map<String, Object> inventoryWithStock = new HashMap<>((GenericValue) inventory);
                            inventoryWithStock.put("stock", BillingAccountWorker.getBillingAccountAvailableBalance(delegator, billingAccountId).longValue() * -1);
                            return inventoryWithStock;
                        } catch (GenericEntityException ignore) {
                            return null;
                        }
                    }
                    return null;
                })
                .collect(Collectors.toList());
    }

    @Authorize(groups = { "FULLADMIN" })
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listProducts",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listProducts(@RequestBody Map<String, Object> payload) throws GenericServiceException, GenericEntityException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "InventoryLookupView", payload);
        return UtilMisc.toMap("products", withStocks((List<?>) result.get("list"), delegator), "count", result.get("listSize"));
    }

    @Authorize
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listPartyProducts",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listPartyProducts(@RequestBody Map<String, Object> payload, @RequestAttribute Map<String, String> signedParty) throws GenericServiceException, GenericEntityException {
        payload.put("partyId", signedParty.get("partyId"));

        Map<String, Object> result = QueryUtil.find(dispatcher, "InventoryLookupView", payload);
        return UtilMisc.toMap("products", withStocks((List<?>) result.get("list"), delegator), "count", result.get("listSize"));
    }
}
