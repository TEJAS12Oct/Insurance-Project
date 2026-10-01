package com.boltpe.insurance.validation;

import java.util.ArrayList;
import java.util.List;

/** Common Adult/Spouse/Child age rules shared by Health and Super Top-Up. */
public final class FamilyAgeValidator {
    private static final int MIN_ADULT_AGE = 18;
    private static final int MIN_PARENT_CHILD_GAP = 21;
    private static final int MIN_CHILD_GAP = 2;
    private FamilyAgeValidator() { }

    public static void validate(Integer age, Integer spouseAge, Integer child1Age, Integer child2Age, Integer child3Age, Integer child4Age) {
        validateAdult(age);
        if (spouseAge != null && spouseAge < MIN_ADULT_AGE) throw new IllegalArgumentException("spouse_age must be at least 18");
        List<ChildAge> children = new ArrayList<>();
        add(children, 1, child1Age); add(children, 2, child2Age); add(children, 3, child3Age); add(children, 4, child4Age);
        for (ChildAge child : children) {
            if (child.age() < 0) throw new IllegalArgumentException("child_" + child.number() + "_age cannot be negative");
            if (age - child.age() < MIN_PARENT_CHILD_GAP) throw new IllegalArgumentException("parent age must be at least 21 years greater than child_" + child.number() + "_age");
            if (spouseAge != null && spouseAge - child.age() < MIN_PARENT_CHILD_GAP) throw new IllegalArgumentException("spouse age must be at least 21 years greater than child_" + child.number() + "_age");
        }
        for (int i=0;i<children.size();i++) for (int j=i+1;j<children.size();j++)
            if (Math.abs(children.get(i).age()-children.get(j).age()) < MIN_CHILD_GAP)
                throw new IllegalArgumentException("minimum age difference between children must be 2 years");
    }

    public static void validateAdult(Integer age) {
        if (age == null) throw new IllegalArgumentException("age is required");
        if (age < MIN_ADULT_AGE) throw new IllegalArgumentException("age must be at least 18");
    }
    private static void add(List<ChildAge> children,int number,Integer age){ if(age!=null) children.add(new ChildAge(number,age)); }
    private record ChildAge(int number,int age) { }
}
