package ir.orix.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchoolPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolPlatformApplication.class, args);
    }
}
