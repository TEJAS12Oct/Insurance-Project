package com.boltpe.insurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

class BoltpeInsuranceApplicationTest {

    @Test void mainStartsSpringWithThisClassAndGivenArgs() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(eq(BoltpeInsuranceApplication.class), any(String[].class)))
                    .thenReturn(mock(ConfigurableApplicationContext.class));

            BoltpeInsuranceApplication.main(args);

            spring.verify(() -> SpringApplication.run(eq(BoltpeInsuranceApplication.class), same(args)));
        }
    }

    @Test void mainPassesEmptyArgsThrough() {
        String[] args = {};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            BoltpeInsuranceApplication.main(args);

            spring.verify(() -> SpringApplication.run(eq(BoltpeInsuranceApplication.class), same(args)));
        }
    }

    @Test void mainPropagatesStartupFailure() {
        String[] args = {};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(eq(BoltpeInsuranceApplication.class), any(String[].class)))
                    .thenThrow(new IllegalStateException("context failed"));

            assertThatThrownBy(() -> BoltpeInsuranceApplication.main(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("context failed");
        }
    }

    @Test void isSpringBootApplicationInRootPackage() {
        assertThat(BoltpeInsuranceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        // Component scanning starts from this package, so it must stay at the root of the service.
        assertThat(BoltpeInsuranceApplication.class.getPackageName()).isEqualTo("com.boltpe.insurance");
    }

    @Test void canBeInstantiatedAsConfigurationBean() {
        // Spring instantiates the @SpringBootApplication class through its no-arg constructor.
        assertThat(new BoltpeInsuranceApplication()).isNotNull();
    }
}
