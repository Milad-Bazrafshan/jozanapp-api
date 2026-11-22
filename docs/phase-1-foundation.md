# فاز یک: زیرساخت فنی

## خروجی

- Spring Boot 4.1.1 روی Java 17 با Maven Wrapper
- PostgreSQL 18.6 و Flyway
- JPA با schema validation و UTC
- Actuator liveness/readiness
- OpenAPI و Swagger UI
- Problem Details، Validation Error و Trace ID یکپارچه
- CORS قابل پیکربندی
- React 19.3 و Vite 8.3 با JavaScript
- دو برنامه مستقل Admin و Client با Router، API client و RTL
- Health Check واقعی از هر دو Frontend به Backend
- lockfile، تنظیمات build و چرخه اجرای مستقل برای هر Frontend
- Dockerfile برای هر سه برنامه و Compose کامل

## دستورات بررسی

```powershell
cd school-backend-api
.\mvnw.cmd verify -B -ntp

cd ..\admin-front
npm run lint
npm run build

cd ..\client-front
npm run lint
npm run build

cd ..\school-backend-api
docker compose -f compose.yml config
```

## بررسی یکپارچه

پس از فعال‌بودن Docker daemon:

```powershell
cd school-backend-api
docker compose -f compose.yml up -d --build
Invoke-RestMethod http://localhost:8080/api/v1/system/health
docker compose -f compose.yml down
```

پاسخ Health باید `status=UP` داشته باشد. سپس دو Frontend با دستورهای مستقل خود اجرا می‌شوند و وضعیت اتصال را نمایش می‌دهند.

## معیار پایان

- Backend compile، test و package شود.
- هر دو Frontend بدون خطای lint، production build شوند.
- Compose معتبر باشد.
- Migration روی PostgreSQL اعمال و Health دیتابیس `UP` شود.
- هر دو Frontend از طریق Vite proxy یا آدرس API تنظیم‌شده پاسخ Health را دریافت کنند.

سه معیار اول تأیید شده‌اند. بررسی runtime سه سرویس پس از فعال‌شدن Docker daemon انجام می‌شود.
