# معماری و مرز ماژول‌ها

## سبک معماری

Backend یک Modular Monolith است: یک برنامه قابل استقرار با ماژول‌های دامنه‌ای دارای مرز روشن. ارتباط درون‌فرایندی مستقیم مجاز است، اما هر ماژول فقط از API عمومی ماژول دیگر استفاده می‌کند و به Repository یا جدول داخلی آن دسترسی ندارد.

```text
admin-front  ─┐
              ├── REST API ── Spring Boot Modular Monolith ── PostgreSQL
client-front ─┘                         │
                                       ├── Object Storage
                                       ├── Notification Providers
                                       └── Payment Providers (future: Orix Wallet)
```

## ماژول‌ها

| ماژول | مالک داده | مسئولیت |
|---|---|---|
| identity | UserAccount، Credential، Session | احراز هویت و چرخه حساب ورود |
| organization | Tenant، Institution، Branch، Membership، Role | مرز داده و دسترسی سازمانی |
| people | Person و Profileها | اطلاعات اشخاص و روابط ولی |
| academic | AcademicPeriod، Program، Course، Offering | کاتالوگ و ساختار آموزشی |
| classroom | ClassSection، InstructorAssignment | هویت و اعضای اجرایی کلاس |
| enrollment | Enrollment و تاریخچه | عضویت، انصراف و انتقال |
| scheduling | ScheduleRule، Session، Location | برنامه، جلسه و کنترل تداخل |
| attendance | AttendanceRecord | حضور و اصلاحات |
| assessment | Question، Assessment، Attempt، Response | بانک سؤال و اجرای آزمون |
| grading | Result، GradeHistory، Publication | تصحیح، نتیجه و انتشار |
| finance | Charge، Invoice، Payment، Allocation | عملیات مالی آموزشی |
| notification | Announcement، Notification، Delivery | اعلان و ارسال |
| integration | Provider Adapter و ExternalIdentity | اتصال سامانه‌های بیرونی |
| audit | AuditLog و OutboxEvent | قابلیت ردیابی و انتشار مطمئن رویداد |

## قواعد وابستگی

- `identity` درباره دانش‌آموز یا استاد اطلاعی ندارد.
- `people` حساب ورود را مالک نیست؛ فقط در صورت نیاز شناسه حساب را از Membership دریافت می‌کند.
- `classroom` جزئیات پرداخت را نمی‌خواند.
- `finance` از رویداد ثبت‌نام برای ایجاد Charge استفاده می‌کند و رکورد Enrollment را تغییر نمی‌دهد.
- `grading` پاسخ‌های آزمون را می‌خواند، اما محتوای Question Version منتشرشده را تغییر نمی‌دهد.
- `notification` رویدادهای سایر ماژول‌ها را مصرف می‌کند و منبع حقیقت کلاس، آزمون یا پرداخت نیست.
- `integration` مدل Orix را وارد دامنه مالی نمی‌کند؛ فقط آن را به قرارداد داخلی Payment Provider ترجمه می‌کند.

## الگوی داخلی پیشنهادی هر ماژول

```text
module/
├── domain/          # قواعد، Aggregateها و Value Objectها
├── application/     # Use caseها، Command/Query و تراکنش
├── infrastructure/  # JPA، Providerها و پیاده‌سازی Portها
└── web/             # Controller و DTOهای HTTP
```

برای CRUDهای ساده می‌توان پوشه‌ها را سبک‌تر کرد، اما جهت وابستگی باید از web و infrastructure به application/domain باشد.

## تراکنش و رویداد

- هر Use Case تغییردهنده یک مرز تراکنش دارد.
- عملیات بین ماژول‌ها که باید همان لحظه موفق شوند با Application Service عمومی انجام می‌شوند.
- اعلان، گزارش و اتصال بیرونی با Domain Event و Transactional Outbox انجام می‌شوند.
- مصرف‌کننده رویداد باید idempotent باشد.
- Event شامل `eventId`، `eventType`، `occurredAt`، `tenantId`، `aggregateType`، `aggregateId` و payload نسخه‌شده است.

## Multi-Tenancy

- مدل MVP از Shared Database و Shared Schema استفاده می‌کند.
- همه جدول‌های تجاری ستون `tenant_id` دارند.
- Tenant فعال از Context احراز‌شده می‌آید و مقدار ارسالی Entity از Client پذیرفته نمی‌شود.
- Repositoryها و Constraintهای یکتا Tenant-aware هستند.
- شناسه تصادفی جای کنترل دسترسی را نمی‌گیرد.
- Jobهای پس‌زمینه و Event Consumerها نیز Tenant Context صریح دارند.
- تست خودکار جداسازی Tenant برای هر ماژول اجباری است.

## احراز هویت و مجوز

- User Account هویت سراسری ورود است.
- Tenant Membership حساب را به Person داخل Tenant متصل می‌کند.
- Token هویت حساب را حمل می‌کند؛ Tenant فعال جداگانه انتخاب و عضویت آن اعتبارسنجی می‌شود.
- Authorization ترکیبی از Permission، Scope و رابطه دامنه‌ای است.
- مثال: `ATTENDANCE_WRITE` به‌تنهایی کافی نیست؛ استاد باید به جلسه آن کلاس نیز تخصیص یافته باشد.

## Frontend

### admin-front

برای مدیران، کارکنان مالی، اپراتور و استاد. دسترسی صفحات و عملیات براساس Permission کنترل می‌شود؛ Backend همچنان مرجع نهایی مجوز است.

### client-front

برای دانش‌آموز و ولی. کاربر دارای چند نقش می‌تواند نمای فعال خود را عوض کند. انتخاب فرزند در پنل ولی نباید باعث اعتماد Backend به شناسه ارسالی شود؛ رابطه فعال ولی دوباره بررسی می‌شود.

### کد مشترک

تا زمانی که نیاز واقعی روشن نشده، پکیج مشترک مستقل ساخته نمی‌شود. قرارداد API می‌تواند از OpenAPI تولید شود؛ Design Token و Componentها پس از شکل‌گیری طراحی پایدار استخراج می‌شوند.

## ذخیره‌سازی

- PostgreSQL منبع حقیقت داده‌های تراکنشی است.
- فایل‌ها در Object Storage نگهداری و فقط metadata و ownership در دیتابیس ثبت می‌شوند.
- Cache در فاز اول شرط صحت نیست و بعداً برای داده قابل بازسازی اضافه می‌شود.
- تاریخچه‌های حساس append-only هستند و حذف فیزیکی عادی ندارند.

## استقرار اولیه

```text
Reverse Proxy
├── admin-front static assets
├── client-front static assets
└── school-backend-api
    ├── PostgreSQL
    └── Object Storage
```

افزودن Message Broker تا زمانی که حجم یا اتصال‌ها ضرورت آن را ثابت نکرده‌اند به تعویق می‌افتد؛ Outbox می‌تواند ابتدا توسط Worker داخل همان برنامه پردازش شود.
