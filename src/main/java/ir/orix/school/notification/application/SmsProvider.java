package ir.orix.school.notification.application;

public interface SmsProvider {
    String code();
    Result send(String phone, String text);
    record Result(boolean ok, String providerMessageId, String error) {}
}
