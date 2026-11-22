# فاز ۵: برنامه کلاس و جلسه

## خروجی

- تعریف محل برگزاری وابسته به شعبه
- تعریف برنامه یک‌باره یا هفتگی با منطقه زمانی IANA
- تخصیص چند استاد و یک محل به برنامه
- کنترل تداخل کلاس، استاد و محل
- تولید idempotent جلسات تا افق حداکثر ۳۶۶ روز
- لغو جلسه با دلیل
- جابه‌جایی جلسه با ساخت جلسه جایگزین و حفظ برنامه اصلی
- نمایش جلسات پیش رو برای دانش‌آموز دارای ثبت‌نام فعال

## مرزهای مدل

`ClassSection` هویت کلاس، `ScheduleRule` الگوی تکرار و `ClassSession` رخداد واقعی است.
اطلاعات روز، ساعت و محل داخل کلاس ذخیره نمی‌شوند. جلسه جابه‌جاشده به رکورد جایگزین اشاره
می‌کند و Rule اصلی تغییر نمی‌کند.

## مجوزها

`PermissionInitializer` در هر بار اجرای بک‌اند، مجوزهای همه فازها را در صورت نبودن ایجاد
می‌کند و همه آن‌ها را به نقش‌های سیستمی می‌دهد. هر دو فرانت نیز هنگام شروع، refresh token
را مصرف می‌کنند تا access token و claimهای مجوز به‌روز شوند.

## APIهای اصلی

- `GET /api/v1/scheduling`
- `POST /api/v1/scheduling/locations`
- `POST /api/v1/scheduling/rules`
- `POST /api/v1/scheduling/rules/{id}/generate`
- `POST /api/v1/scheduling/sessions/{id}/cancel`
- `POST /api/v1/scheduling/sessions/{id}/reschedule`
- `GET /api/v1/scheduling/me`
