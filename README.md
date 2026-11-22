# Orix School Backend API

Backend مستقل سامانه مدیریت مراکز آموزشی با Spring Boot، PostgreSQL و Flyway.

## پیش‌نیاز

- Java 17 یا بالاتر
- Docker برای PostgreSQL

نصب Maven سراسری لازم نیست؛ Wrapper همراه پروژه قرار دارد.

## اجرای محیط توسعه

فایل `.env.example` را با نام `.env` کپی و رمز دیتابیس را تغییر دهید، سپس:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

آدرس‌ها:

- API health: http://localhost:8080/api/v1/system/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator readiness: http://localhost:8080/actuator/health/readiness

با `BOOTSTRAP_ADMIN_ENABLED=true` حساب توسعه با شناسه مجموعه `demo-school`، ایمیل
`admin@example.com` و رمز `change-me-now` ساخته می‌شود.

اگر `DEMO_DATA_ENABLED=true` باشد (پیش‌فرض محیط محلی)، پس از bootstrap یک مجموعه داده
نمونه‌ای هم ساخته می‌شود: استاد، سه دانش‌آموز، دو ولی، کلاس زبان و مکالمه، برنامه هفتگی،
حضور و غیاب امروز و یک آزمونک منتشرشده. ورود پنل دانش‌آموز/ولی:

- `student1@example.com` / `change-me-now` (سارا احمدی)
- `student2@example.com` / `change-me-now` (رضا کریمی)
- `student3@example.com` / `change-me-now` (نیلا محمدی)
- `guardian@example.com` / `change-me-now` (ولی سارا و رضا)
- `guardian2@example.com` / `change-me-now` (ولی نیلا)
- `instructor@example.com` / `change-me-now` (استاد مینا رضایی)

Seed فقط یک‌بار و در صورت نبودن دانش‌آموز `DEMO-STU-001` اجرا می‌شود. جزئیات فاز هویت و سازمان در
[docs/phase-2-identity-organization.md](docs/phase-2-identity-organization.md) آمده است.

ثبت دانش‌آموز، استاد، ولی و کاتالوگ آموزشی در
[docs/phase-3-people-academic.md](docs/phase-3-people-academic.md) مستند شده است.

ارائه درس، کلاس، تخصیص استاد و ثبت‌نام در
[docs/phase-4-classroom-enrollment.md](docs/phase-4-classroom-enrollment.md) قرار دارد.

برنامه هفتگی، محل برگزاری، تولید و جابه‌جایی جلسات در
[docs/phase-5-scheduling.md](docs/phase-5-scheduling.md) مستند شده است.

ثبت، نهایی‌سازی، اصلاح و مشاهده حضور و غیاب در
[docs/phase-6-attendance.md](docs/phase-6-attendance.md) مستند شده است.

بانک سؤال نسخه‌دار، طراحی آزمون و انتشار Snapshot در
[docs/phase-7-assessment-builder.md](docs/phase-7-assessment-builder.md) مستند شده است.

مجوزهای پایه هنگام هر بار اجرای برنامه تکمیل می‌شوند. برای دریافت claimهای جدید پس از
به‌روزرسانی بک‌اند، صفحه پنل را یک‌بار reload کنید؛ فرانت access token را خودکار refresh می‌کند.

## اجرای کامل Backend با Docker

```powershell
docker compose up --build
```

## تست و build

```powershell
.\mvnw.cmd verify -B -ntp
```

مستندات دامنه و معماری در [docs](docs/README.md) قرار دارند.
