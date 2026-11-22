package ir.orix.school.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ir.orix.school.notification.application.SmsProvider;

@Component
public class DemoSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(DemoSmsProvider.class);
    @Override public String code() { return "DEMO"; }
    @Override public Result send(String phone, String text) {
        log.info("Demo SMS to {} :: {}", phone, text);
        return new Result(true, "demo-" + Math.abs(text.hashCode()), null);
    }
}
