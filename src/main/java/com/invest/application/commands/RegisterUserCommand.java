package com.invest.application.commands;

import com.invest.domain.entities.enumerator.SubscriptionPlan;

public record RegisterUserCommand(String name, String email, String password, SubscriptionPlan subscriptionPlan) {

    public RegisterUserCommand(String name, String email, String password) {
        this(name, email, password, null);
    }
}
