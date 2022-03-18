package OfbizSpring.Annotations;

public @interface Authorize {
    String[] role() default {};
}
