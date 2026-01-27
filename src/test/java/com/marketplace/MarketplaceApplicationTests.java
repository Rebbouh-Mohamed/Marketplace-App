package com.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class MarketplaceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void verifiesModularStructure() {
        ApplicationModules modules = ApplicationModules.of(MarketplaceApplication.class);
        modules.verify();
    }
}
