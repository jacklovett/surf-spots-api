package com.lovettj.surfspotsapi.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DirectionValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDirection {

    String message() default "Invalid direction format. Use 'NE - SE'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
