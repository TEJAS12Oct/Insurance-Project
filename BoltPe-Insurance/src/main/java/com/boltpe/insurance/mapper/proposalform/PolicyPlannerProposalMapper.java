package com.boltpe.insurance.mapper.proposalform;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps the BoltPe proposal wrapper to the Policy Planner request body.
 *
 * BoltPe accepts:
 * client_name, formType, insuranceType, data { ... }
 *
 * Policy Planner receives:
 * client_name, formType, plus every field from data at the top level.
 * insuranceType and the data wrapper are not sent to Policy Planner.
 *
 * BoltPe accepts DD-MM-YYYY dates while Policy Planner requires YYYY-MM-DD.
 */
@Component
public class PolicyPlannerProposalMapper {
    private static final String FIELD_ANNUAL_INCOME = "annualIncome";

    private static final DateTimeFormatter BOLTPE_DATE =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter POLICY_PLANNER_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Frontend annual-income slab code -> representative annual income sent to Policy Planner.
     * Closed slabs use their midpoint. The open-ended 40L+ slab uses the agreed fixed value of 50L.
     */
    private static final Map<String, BigDecimal> ANNUAL_INCOME_BY_SLAB = Map.of(
            "3L_5L", BigDecimal.valueOf(400000),
            "5L_8L", BigDecimal.valueOf(650000),
            "8L_12L", BigDecimal.valueOf(1000000),
            "12L_18L", BigDecimal.valueOf(1500000),
            "18L_25L", BigDecimal.valueOf(2150000),
            "25L_40L", BigDecimal.valueOf(3250000),
            "40L_ABOVE", BigDecimal.valueOf(5000000));

    public Map<String, Object> toVendorRequest(InsuranceProposalRequest request) {
        Map<String, Object> data = extractData(request);

        Map<String, Object> vendorBody = new LinkedHashMap<>();
        vendorBody.put("client_name", request.getClientName());
        vendorBody.put("formType", request.getFormType());
        data.forEach((key, value) -> vendorBody.put(key, deepCopyValue(value)));

        mapAnnualIncomeSlab(vendorBody);

        String formType = request.getFormType() == null ? "" : request.getFormType().toLowerCase();
        if ("pa".equals(formType)) {
            normalizeIndianMobile(vendorBody, "mobileNumber");
            convertDate(vendorBody, "dateOfBirth");
        } else {
            normalizeIndianMobile(vendorBody, "proposerPhone");
            convertDate(vendorBody, "proposerDOB");
            convertMemberDates(vendorBody, "adults");
            convertMemberDates(vendorBody, "children");
        }
        return vendorBody;
    }

    private Map<String, Object> extractData(InsuranceProposalRequest request) {
        Object rawData = request.getFields().get("data");
        if (!(rawData instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("data is required");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> data.put(String.valueOf(key), value));
        return data;
    }


    private void mapAnnualIncomeSlab(Map<String, Object> body) {
        if (!body.containsKey(FIELD_ANNUAL_INCOME) || body.get(FIELD_ANNUAL_INCOME) == null) {
            return;
        }

        Object rawValue = body.get(FIELD_ANNUAL_INCOME);
        if (!(rawValue instanceof String slab) || slab.isBlank()) {
            throw new IllegalArgumentException("Invalid annualIncome slab");
        }

        BigDecimal mappedValue = ANNUAL_INCOME_BY_SLAB.get(slab.trim().toUpperCase());
        if (mappedValue == null) {
            throw new IllegalArgumentException("Invalid annualIncome slab");
        }
        body.put(FIELD_ANNUAL_INCOME, mappedValue);
    }

    private void normalizeIndianMobile(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof String text && text.startsWith("+91") && text.length() == 13) {
            body.put(field, text.substring(3));
        }
    }

    private void convertMemberDates(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object member : list) {
            if (member instanceof Map<?, ?> rawMember) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memberMap = (Map<String, Object>) rawMember;
                convertDate(memberMap, "dob");
            }
        }
    }

    private void convertDate(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) {
            return;
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(field + " must be in DD-MM-YYYY format");
        }
        try {
            LocalDate date = LocalDate.parse(text, BOLTPE_DATE);
            body.put(field, POLICY_PLANNER_DATE.format(date));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be in DD-MM-YYYY format");
        }
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> nested.put(String.valueOf(key), deepCopyValue(nestedValue)));
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> nested = new ArrayList<>(list.size());
            list.forEach(item -> nested.add(deepCopyValue(item)));
            return nested;
        }
        return value;
    }
}
