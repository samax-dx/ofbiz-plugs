package OfbizSpring.Controller;

import OfbizSpring.Util.QueryUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Product")
public class Product {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/listProducts",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object listProducts(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        Map<String, Object> result = QueryUtil.find(dispatcher, "ProductLookupView", payload);
        return UtilMisc.toMap("products", result.get("list"), "count", result.get("listSize"));
    }
}
