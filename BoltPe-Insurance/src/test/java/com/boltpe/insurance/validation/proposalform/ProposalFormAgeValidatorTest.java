package com.boltpe.insurance.validation.proposalform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boltpe.insurance.dto.request.proposalform.AdultMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.ChildMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProposalFormAgeValidatorTest {
    private ProposalFormAgeValidator validator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
        validator = new ProposalFormAgeValidator(clock);
    }

    @Test
    void acceptsValidSelfSpouseFatherMotherAndAllFourChildren() {
        GroupProposalRequest request = request(
                "29-09-1986", // self 40
                List.of(
                        adult("self", "29-09-1986"),
                        adult("spouse", "29-09-1991"), // 35
                        adult("father", "29-09-1965"), // 61
                        adult("mother", "29-09-1965")  // 61
                ),
                List.of(
                        child("29-09-2012"), // 14
                        child("29-09-2014"), // 12
                        child("29-09-2016"), // 10
                        child("29-09-2018")  // 8
                ));

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsSelfAt18And80Boundaries() {
        assertThatCode(() -> validator.validate(request("29-09-2008", List.of(), List.of())))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(request("29-09-1946", List.of(), List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSelfBelow18AndAbove80() {
        assertFieldError(request("30-09-2008", List.of(), List.of()),
                "proposerDOB", "Self age must be between 18 and 80 years");
        assertFieldError(request("28-09-1945", List.of(), List.of()),
                "proposerDOB", "Self age must be between 18 and 80 years");
    }

    @Test
    void rejectsInvalidBlankAndFutureSelfDob() {
        assertFieldError(request("31-02-2000", List.of(), List.of()),
                "proposerDOB", "DOB must be in DD-MM-YYYY format");
        assertFieldError(request("", List.of(), List.of()),
                "proposerDOB", "Self DOB is required");
        assertFieldError(request("30-09-2026", List.of(), List.of()),
                "proposerDOB", "Future DOB is not allowed");
    }

    @Test
    void validatesSpouseAgeRangeButNoSelfSpouseGap() {
        assertThatCode(() -> validator.validate(request(
                "29-09-1996", List.of(adult("spouse", "29-09-1986")), List.of())))
                .doesNotThrowAnyException();

        assertFieldError(request("29-09-1996", List.of(adult("spouse", "29-09-2009")), List.of()),
                "adults[0].dob", "Spouse age must be between 18 and 80 years");
    }

    @Test
    void validatesFatherMustBe21YearsOlderThanSelf() {
        assertThatCode(() -> validator.validate(request(
                "29-09-1996", List.of(adult("father", "29-09-1975")), List.of())))
                .doesNotThrowAnyException();

        assertFieldError(request("29-09-1996", List.of(adult("father", "29-09-1976")), List.of()),
                "adults[0].dob", "Father age must be at least 21 years greater than self age");
    }

    @Test
    void validatesMotherMustBe21YearsOlderThanSelf() {
        assertThatCode(() -> validator.validate(request(
                "29-09-1998", List.of(adult("mother", "29-09-1977")), List.of())))
                .doesNotThrowAnyException();

        assertFieldError(request("29-09-1998", List.of(adult("mother", "29-09-1978")), List.of()),
                "adults[0].dob", "Mother age must be at least 21 years greater than self age");
    }

    @Test
    void enforcesFatherAndMotherMaximum80BeforeGapRule() {
        GroupProposalRequest request = request("29-09-1986",
                List.of(adult("father", "28-09-1945"), adult("mother", "28-09-1945")), List.of());

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalAgeValidationException.class)
                .satisfies(ex -> {
                    ProposalAgeValidationException p = (ProposalAgeValidationException) ex;
                    assertThat(p.getFieldErrors())
                            .containsEntry("adults[0].dob", "Father age must be between 18 and 80 years")
                            .containsEntry("adults[1].dob", "Mother age must be between 18 and 80 years");
                });
    }

    @Test
    void validatesSelfToEachChild21YearGap() {
        assertThatCode(() -> validator.validate(request(
                "29-09-1986", List.of(), List.of(child("29-09-2007")))))
                .doesNotThrowAnyException();

        assertFieldError(request("29-09-1986", List.of(), List.of(child("29-09-2006"))),
                "children[0].dob", "Self age must be at least 21 years greater than child age");
    }

    @Test
    void validatesSpouseToEachChild21YearGap() {
        GroupProposalRequest valid = request("29-09-1981",
                List.of(adult("spouse", "29-09-1986")),
                List.of(child("29-09-2007")));
        assertThatCode(() -> validator.validate(valid)).doesNotThrowAnyException();

        GroupProposalRequest invalid = request("29-09-1981",
                List.of(adult("spouse", "29-09-1987")),
                List.of(child("29-09-2007")));
        assertFieldError(invalid, "children[0].dob",
                "Spouse age must be at least 21 years greater than child age");
    }

    @Test
    void acceptsNewbornChild() {
        GroupProposalRequest request = request("29-09-1996", List.of(), List.of(child("29-09-2026")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsCorrectDescendingChildSequence() {
        GroupProposalRequest request = request("29-09-1976", List.of(adult("spouse", "29-09-1979")),
                List.of(child("29-09-2006"), child("29-09-2008"), child("29-09-2010"), child("29-09-2012")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsC1C2AdjacentSameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(adult("spouse", "29-09-1979")),
                List.of(child("29-09-2006"), child("29-09-2006"), child("29-09-2008"), child("29-09-2010")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsC2C3AdjacentSameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(adult("spouse", "29-09-1979")),
                List.of(child("29-09-2006"), child("29-09-2007"), child("29-09-2007"), child("29-09-2009")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsC3C4AdjacentSameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(adult("spouse", "29-09-1979")),
                List.of(child("29-09-2006"), child("29-09-2007"), child("29-09-2008"), child("29-09-2008")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsTwoSeparateAdjacentSameAgePairs() {
        GroupProposalRequest request = request("29-09-1976", List.of(adult("spouse", "29-09-1979")),
                List.of(child("29-09-2006"), child("29-09-2006"), child("29-09-2008"), child("29-09-2008")));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsC1C3SameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2006"), child("29-09-2007"), child("29-09-2006"), child("29-09-2008")));
        assertFieldError(request, "children[2].dob",
                "Non-adjacent children cannot have the same age");
    }

    @Test
    void rejectsC1C4SameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2006"), child("29-09-2007"), child("29-09-2008"), child("29-09-2006")));
        assertFieldError(request, "children[3].dob",
                "Non-adjacent children cannot have the same age");
    }

    @Test
    void rejectsC2C4SameAge() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2006"), child("29-09-2007"), child("29-09-2008"), child("29-09-2007")));
        assertFieldError(request, "children[3].dob",
                "Non-adjacent children cannot have the same age");
    }

    @Test
    void rejectsNonAdjacentSameAgeEvenWhenSequenceDoesNotIncrease() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2006"), child("29-09-2006"), child("29-09-2006"), child("29-09-2008")));
        assertFieldError(request, "children[2].dob", "Non-adjacent children cannot have the same age");
    }

    @Test
    void rejectsC2OlderThanC1() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2008"), child("29-09-2007"), child("29-09-2009"), child("29-09-2010")));
        assertFieldError(request, "children[1].dob",
                "Child ages must follow the sequence Child 1 >= Child 2 >= Child 3 >= Child 4");
    }

    @Test
    void rejectsAllFourChildrenSameAgeBecauseNonAdjacentPairsMatch() {
        GroupProposalRequest request = request("29-09-1976", List.of(),
                List.of(child("29-09-2006"), child("29-09-2006"), child("29-09-2006"), child("29-09-2006")));
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalAgeValidationException.class)
                .satisfies(ex -> assertThat(((ProposalAgeValidationException) ex).getFieldErrors())
                        .containsValue("Non-adjacent children cannot have the same age"));
    }

    @Test
    void supportsNonSequentialChildPositionsAndKeepsOriginalIndexes() {
        GroupProposalRequest valid = request("29-09-1976", List.of(),
                Arrays.asList(child("29-09-2006"), null, child("29-09-2008"), null));
        assertThatCode(() -> validator.validate(valid)).doesNotThrowAnyException();

        GroupProposalRequest invalid = request("29-09-1976", List.of(),
                Arrays.asList(child("29-09-2006"), null, child("29-09-2006"), null));
        assertFieldError(invalid, "children[2].dob", "Non-adjacent children cannot have the same age");
    }

    @Test
    void requiresDobForProvidedSpouseFatherMotherAndChild() {
        GroupProposalRequest request = request("29-09-1996",
                List.of(adult("spouse", null), adult("father", ""), adult("mother", null)),
                List.of(child(null)));

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalAgeValidationException.class)
                .satisfies(ex -> {
                    ProposalAgeValidationException p = (ProposalAgeValidationException) ex;
                    assertThat(p.getFieldErrors())
                            .containsEntry("adults[0].dob", "Spouse DOB is required")
                            .containsEntry("adults[1].dob", "Father DOB is required")
                            .containsEntry("adults[2].dob", "Mother DOB is required")
                            .containsEntry("children[0].dob", "Child DOB is required");
                });
    }

    @Test
    void ignoresUnknownAdultRelationshipAndBlankRelationship() {
        GroupProposalRequest request = request("29-09-1996",
                List.of(adult("brother", null), adult(null, null)),
                List.of());
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void validatesAdultSelfEntryWhenPresent() {
        assertFieldError(request("29-09-1996", List.of(adult("self", "29-09-2010")), List.of()),
                "adults[0].dob", "Self age must be between 18 and 80 years");
    }

    @Test
    void collectsParentAndChildSequenceErrorsTogether() {
        GroupProposalRequest request = request("29-09-1986",
                List.of(adult("father", "29-09-1970")),
                List.of(child("29-09-2007"), child("29-09-2006")));

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalAgeValidationException.class)
                .satisfies(ex -> {
                    ProposalAgeValidationException p = (ProposalAgeValidationException) ex;
                    assertThat(p.getFieldErrors())
                            .containsEntry("adults[0].dob", "Father age must be at least 21 years greater than self age")
                            .containsEntry("children[1].dob", "Self age must be at least 21 years greater than child age");
                });
    }

    private void assertFieldError(GroupProposalRequest request, String field, String message) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalAgeValidationException.class)
                .satisfies(ex -> assertThat(((ProposalAgeValidationException) ex).getFieldErrors())
                        .containsEntry(field, message));
    }

    private AdultMemberRequest adult(String relationship, String dob) {
        return new AdultMemberRequest(relationship, null, "Member", dob,
                null, null, null, null, null);
    }

    private ChildMemberRequest child(String dob) {
        return new ChildMemberRequest("child", "Child", dob,
                null, null, null);
    }

    private GroupProposalRequest request(String proposerDob,
                                         List<AdultMemberRequest> adults,
                                         List<ChildMemberRequest> children) {
        return new GroupProposalRequest(
                "Health Insurance", "New", "110001", "Delhi, Delhi", "A", "No",
                1, children.size(), "Sample Adult", proposerDob, "male", "sample@example.com",
                "9000000000", BigDecimal.valueOf(500000), BigDecimal.ONE,
                "5L_8L", "No", "Yes", "No",
                "000000000000", "ABCDE1234F", BigDecimal.valueOf(12000),
                BigDecimal.valueOf(14160), adults, children);
    }
}
