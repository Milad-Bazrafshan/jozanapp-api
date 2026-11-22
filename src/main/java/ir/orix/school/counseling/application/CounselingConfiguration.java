package ir.orix.school.counseling.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CounselingAiProperties.class)
public class CounselingConfiguration {}
