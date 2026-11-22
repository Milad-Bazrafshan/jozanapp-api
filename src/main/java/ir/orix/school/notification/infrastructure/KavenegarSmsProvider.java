package ir.orix.school.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ir.orix.school.notification.application.SmsProvider;

@Component
public class KavenegarSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(KavenegarSmsProvider.class);
    @Override public String code() { return "KAVENEGAR"; }
    @Override public Result send(String phone, String text) {
        log.info("Kavenegar adapter (demo fallback) to {} :: {}", phone, text);
        return new Result(true, "kavenegar-demo", null);
    }
}
