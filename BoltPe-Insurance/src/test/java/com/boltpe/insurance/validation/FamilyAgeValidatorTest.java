package com.boltpe.insurance.validation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FamilyAgeValidatorTest {
    @Test void acceptsAdultOnly() { assertDoesNotThrow(() -> FamilyAgeValidator.validate(40, null, null, null, null, null)); }
    @Test void rejectsAdultBelow18() { assertEquals("age must be at least 18", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(17,null,null,null,null,null)).getMessage()); }
    @Test void rejectsSpouseBelow18() { assertEquals("spouse_age must be at least 18", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(40,17,null,null,null,null)).getMessage()); }
    @Test void acceptsChildOlderThan25WhenGapIsValid() { assertDoesNotThrow(() -> FamilyAgeValidator.validate(55,null,34,null,null,null)); }
    @Test void rejectsNegativeChild() { assertEquals("child_1_age cannot be negative", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(40,null,-1,null,null,null)).getMessage()); }
    @Test void rejectsAdultChildGapBelow21() { assertEquals("parent age must be at least 21 years greater than child_1_age", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(40,null,20,null,null,null)).getMessage()); }
    @Test void rejectsSpouseChildGapBelow21() { assertEquals("spouse age must be at least 21 years greater than child_1_age", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(55,50,30,null,null,null)).getMessage()); }
    @Test void rejectsChildPairGapBelow2() { assertEquals("minimum age difference between children must be 2 years", assertThrows(IllegalArgumentException.class, () -> FamilyAgeValidator.validate(50,null,20,19,null,null)).getMessage()); }
    @Test void supportsNonSequentialChildren() { assertDoesNotThrow(() -> FamilyAgeValidator.validate(50,null,20,null,16,null)); }
}
