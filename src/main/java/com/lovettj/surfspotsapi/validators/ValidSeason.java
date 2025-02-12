package com.lovettj.surfspotsapi.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SeasonValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSeason {

    String message() default "Invalid season format. Use 'April - September'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
