package OfbizSpring.Aspects;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import OfbizSpring.Annotations.Authorize;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
public class AuthorizationAspect {
    private final Delegator delegator;
    private final LocalDispatcher dispatcher;

    public AuthorizationAspect(Delegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
    }

    @Around(value = "@annotation(authorizeAnnotation)")
    public Object authorize(ProceedingJoinPoint joinPoint, Authorize authorizeAnnotation) {
        try {
            Map<String, String> party = UtilMisc.toMap(
                    ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                            .getRequest()
                            .getAttribute("signedParty")
            );

            boolean authChecks;

            authChecks = party.containsKey("partyId") && party.containsKey("loginId") && party.containsKey("exp");
            authChecks &= hasPartyRoles(authorizeAnnotation.roles(), party);
            authChecks &= belongsToPartyGroups(authorizeAnnotation.groups(), party);

            if (authChecks) {
                return joinPoint.proceed();
            } else {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Throwable e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean hasPartyRoles(String[] partyRoles, Map<String, String> party) {
        return true;
    }

    private boolean belongsToPartyGroups(String[] partyGroups, Map<String, String> party) {
        try {
            List<String> foundPartyGroups = EntityQuery.use(delegator)
                    .from("UserLoginSecurityGroup")
                    .where("userLoginId", party.get("loginId"))
                    .queryList()
                    .stream()
                    .map(v -> (String) v.get("groupId"))
                    .collect(Collectors.toList());

            return Arrays.stream(partyGroups).allMatch(foundPartyGroups::contains);
        } catch (GenericEntityException e) {
            return false;
        }
    }
}
