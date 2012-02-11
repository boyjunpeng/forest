package com.jayway.forest.samples.bank.spring.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jayway.forest.legacy.constraint.Constraint;
import com.jayway.forest.legacy.constraint.ConstraintEvaluator;
import com.jayway.forest.samples.bank.model.Withdrawable;
import com.jayway.forest.samples.bank.spring.ResourceWithAccount;

/**
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(IsWithdrawable.Evaluator.class)
public @interface IsWithdrawable {

    class Evaluator implements ConstraintEvaluator<IsWithdrawable, ResourceWithAccount> {

        public boolean isValid( IsWithdrawable role, ResourceWithAccount resource ) {
            return resource.getAccount() instanceof Withdrawable;
        }

    }

}
