package ir.orix.school.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ir.orix.school.notification.application.SmsProvider;

@Component
public class QasedakSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(QasedakSmsProvider.class);
    @Override public String code() { return "QASEDAK"; }
    @Override public Result send(String phone, String text) {
        log.info("Qasedak adapter (demo fallback) to {} :: {}", phone, text);
        return new Result(true, "qasedak-demo", null);
    }
}
