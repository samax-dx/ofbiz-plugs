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

    private void updateInventoryStocks(Map<String, Object> queryData) throws GenericServiceException, GenericEntityException {
        Map<String, Object> filterPayload = queryData
                .entrySet()
                .stream()
                .filter(v -> !v.getKey().matches("^stock(?:_fld\\d_)?"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Object> filterResult = QueryUtil.find(dispatcher, "InventoryLookupView", filterPayload);

        if (filterResult.get("list") instanceof List) {
            List<GenericValue> billingAccounts = ((List<?>) filterResult.get("list"))
                    .stream()
                    .map(v -> {
                        String billingAccountId = (String) UtilMisc.toMap(v).get("inventoryId");
                        try {
                            long balance = BillingAccountWorker.getBillingAccountAvailableBalance(delegator, billingAccountId).longValue() * -1;
                            return delegator.makeValue("BillingAccount", "billingAccountId", billingAccountId, "balance", balance);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            delegator.storeAll(billingAccounts);
        }
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
        updateInventoryStocks(payload);

        Map<String, Object> result = QueryUtil.find(dispatcher, "InventoryLookupView", payload);
        return UtilMisc.toMap("products", result.get("list"), "count", result.get("listSize"));
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
        updateInventoryStocks(payload);

        Map<String, Object> result = QueryUtil.find(dispatcher, "InventoryLookupView", payload);
        return UtilMisc.toMap("products", result.get("list"), "count", result.get("listSize"));
    }
}
