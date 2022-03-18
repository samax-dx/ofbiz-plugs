package OfbizSpring.Controller;

import OfbizSpring.Annotations.OfbizService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.io.json.JsonValue;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@RestController
@RequestMapping("/Party")
public class Party {
    @Autowired
    private Delegator delegator;

    @Autowired
    private LocalDispatcher dispatcher;

    /*@CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/createParty",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> payload) throws GenericServiceException {
        return dispatcher.runSync("spCreateParty", payload);
    }*/

    @OfbizService
    @CrossOrigin(origins = "*")
    @RequestMapping(
            value = "/findParties",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public Object findParties(Map<String, Object> request, HttpServletResponse response) throws GenericServiceException {
        return dispatcher.runSync("spFindParties", request);
    }
}
