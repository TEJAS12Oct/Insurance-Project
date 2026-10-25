package com.boltpe.insurance.validation.proposalform;

import com.boltpe.insurance.dto.request.proposalform.AdultMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.ChildMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProposalFormAgeValidator {
    private static final int ADULT_MIN_AGE = 18;
    private static final int ADULT_MAX_AGE = 80;
    private static final int PARENT_CHILD_MIN_GAP = 21;

    private static final DateTimeFormatter BOLTPE_DATE =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProposalFormAgeValidator() {
        this(Clock.systemDefaultZone());
    }

    ProposalFormAgeValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(GroupProposalRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(clock);
        Integer selfAge = parseRequiredAdultAge(request.proposerDOB(), "proposerDOB", "Self", today, errors);
        Integer spouseAge = validateAdults(request.adults(), selfAge, today, errors);
        List<ChildAge> children = validateChildren(request.children(), selfAge, spouseAge, today, errors);
        validateChildSequence(children, errors);
        if (!errors.isEmpty()) {
            throw new ProposalAgeValidationException(errors);
        }
    }

    private Integer validateAdults(List<AdultMemberRequest> adults, Integer selfAge,
                                   LocalDate today, Map<String, String> errors) {
        Integer spouseAge = null;
        for (int i = 0; i < adults.size(); i++) {
            AdultMemberRequest adult = adults.get(i);
            if (adult == null || normalize(adult.relationship()).isEmpty()) {
                continue;
            }
            String relationship = normalize(adult.relationship());
            String field = "adults[" + i + "].dob";
            if ("spouse".equals(relationship)) {
                spouseAge = parseRequiredAdultAge(adult.dob(), field, "Spouse", today, errors);
            } else {
                validateOtherAdult(adult, relationship, field, selfAge, today, errors);
            }
        }
        return spouseAge;
    }

    private void validateOtherAdult(AdultMemberRequest adult, String relationship, String field,
                                    Integer selfAge, LocalDate today, Map<String, String> errors) {
        switch (relationship) {
            case "self" -> parseRequiredAdultAge(adult.dob(), field, "Self", today, errors);
            case "father", "mother" -> validateParentAge(adult, relationship, field, selfAge, today, errors);
            default -> { /* Only supported relationships require DOB checks here. */ }
        }
    }

    private void validateParentAge(AdultMemberRequest adult, String relationship, String field,
                                    Integer selfAge, LocalDate today, Map<String, String> errors) {
        String label = "father".equals(relationship) ? "Father" : "Mother";
        Integer parentAge = parseRequiredAdultAge(adult.dob(), field, label, today, errors);
        if (parentAge != null && selfAge != null && parentAge - selfAge < PARENT_CHILD_MIN_GAP) {
            errors.put(field, label + " age must be at least 21 years greater than self age");
        }
    }

    private List<ChildAge> validateChildren(List<ChildMemberRequest> children, Integer selfAge,
                                             Integer spouseAge, LocalDate today, Map<String, String> errors) {
        List<ChildAge> parsedChildren = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            ChildMemberRequest child = children.get(i);
            if (child != null) {
                String field = "children[" + i + "].dob";
                Integer childAge = parseChildAge(child.dob(), field, today, errors);
                if (childAge != null) {
                    parsedChildren.add(new ChildAge(i, childAge, field));
                    validateChildParentGaps(childAge, selfAge, spouseAge, field, errors);
                }
            }
        }
        return parsedChildren;
    }

    private void validateChildParentGaps(Integer childAge, Integer selfAge, Integer spouseAge,
                                          String field, Map<String, String> errors) {
        if (selfAge != null && selfAge - childAge < PARENT_CHILD_MIN_GAP) {
            errors.put(field, "Self age must be at least 21 years greater than child age");
        } else if (spouseAge != null && spouseAge - childAge < PARENT_CHILD_MIN_GAP) {
            errors.put(field, "Spouse age must be at least 21 years greater than child age");
        }
    }

    private void validateChildSequence(List<ChildAge> children, Map<String, String> errors) {
        for (int i = 0; i < children.size(); i++) {
            ChildAge older = children.get(i);
            for (int j = i + 1; j < children.size(); j++) {
                ChildAge younger = children.get(j);

                // Child 1 must be oldest and Child 4 youngest.
                if (younger.age() > older.age()) {
                    errors.putIfAbsent(younger.field(),
                            "Child ages must follow the sequence Child 1 >= Child 2 >= Child 3 >= Child 4");
                } else if (younger.age() == older.age()
                        && younger.index() - older.index() > 1) {
                    // Only adjacent child positions may share a completed age.
                    errors.putIfAbsent(younger.field(),
                            "Non-adjacent children cannot have the same age");
                }
            }
        }
    }

    private Integer parseRequiredAdultAge(String dob,
                                          String field,
                                          String memberName,
                                          LocalDate today,
                                          Map<String, String> errors) {
        LocalDate birthDate = parseDob(dob, field, memberName + " DOB is required", errors);
        if (birthDate == null) {
            return null;
        }
        Integer age = calculateAge(birthDate, today, field, errors);
        if (age == null) {
            return null;
        }
        if (age < ADULT_MIN_AGE || age > ADULT_MAX_AGE) {
            errors.put(field, memberName + " age must be between 18 and 80 years");
            return null;
        }
        return age;
    }

    private Integer parseChildAge(String dob,
                                  String field,
                                  LocalDate today,
                                  Map<String, String> errors) {
        LocalDate birthDate = parseDob(dob, field, "Child DOB is required", errors);
        if (birthDate == null) {
            return null;
        }
        return calculateAge(birthDate, today, field, errors);
    }

    private LocalDate parseDob(String value,
                               String field,
                               String requiredMessage,
                               Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            errors.put(field, requiredMessage);
            return null;
        }
        try {
            return LocalDate.parse(value, BOLTPE_DATE);
        } catch (DateTimeParseException ex) {
            errors.put(field, "DOB must be in DD-MM-YYYY format");
            return null;
        }
    }

    private Integer calculateAge(LocalDate birthDate,
                                 LocalDate today,
                                 String field,
                                 Map<String, String> errors) {
        if (birthDate.isAfter(today)) {
            errors.put(field, "Future DOB is not allowed");
            return null;
        }
        return Period.between(birthDate, today).getYears();
    }

    private String normalize(String relationship) {
        return relationship == null ? "" : relationship.trim().toLowerCase(Locale.ROOT);
    }

    private record ChildAge(int index, int age, String field) {
    }
}
