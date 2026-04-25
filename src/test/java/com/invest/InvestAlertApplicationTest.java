package com.invest;

import org.junit.jupiter.api.Test;

class InvestAlertApplicationTest {

    @Test
    void contextLoadsMainClass() {
        // Verifies the main class exists and is loadable
        var app = new InvestAlertApplication();
        assert app != null;
    }
}
