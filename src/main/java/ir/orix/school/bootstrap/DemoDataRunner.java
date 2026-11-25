package ir.orix.school.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@EnableConfigurationProperties(DemoDataProperties.class)
public class DemoDataRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);
    private final DemoDataProperties properties;
    private final DemoDataService service;

    public DemoDataRunner(DemoDataProperties properties, DemoDataService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            service.initialize(properties);
        } catch (Exception exception) {
            log.error("Demo data seed failed", exception);
            if (properties.failFast()) {
                throw new IllegalStateException("Demo data seed failed", exception);
            }
        }
    }
}
