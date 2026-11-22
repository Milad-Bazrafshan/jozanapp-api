# قراردادهای API

## اصول

- API به‌صورت REST و با پیشوند `/api/v1` منتشر می‌شود.
- JSON قالب تبادل اصلی است.
- نام فیلدها `camelCase` و مقدار enumها `UPPER_SNAKE_CASE` هستند.
- شناسه‌ها به شکل UUID string منتقل می‌شوند.
- DTOهای API از Entityهای Persistence جدا هستند.
- breaking change نیازمند نسخه جدید API است.

## Tenant Context

- Client، Tenant فعال را با header به نام `X-Tenant-Id` ارسال می‌کند.
- Backend عضویت فعال حساب در Tenant را در هر درخواست بررسی می‌کند.
- `tenantId` داخل payload موجودیت‌های تجاری پذیرفته نمی‌شود.
- Endpointهای عمومی مانند login و refresh از این header مستثنا هستند.

## پاسخ موفق

برای یک Resource، همان DTO بازگردانده می‌شود:

```json
{
  "id": "01990d75-1e49-7d61-a29d-1d73501ac033",
  "title": "English B2 - Evening",
  "status": "ACTIVE"
}
```

برای collection:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

برای فهرست‌های بزرگ یا رخدادمحور، Cursor Pagination می‌تواند به‌جای Page استفاده شود، اما یک Endpoint نباید هر دو روش را مخلوط کند.

## خطا

خطاها براساس Problem Details برگردانده می‌شوند:

```json
{
  "type": "https://school.orix.ir/problems/schedule-conflict",
  "title": "Schedule conflict",
  "status": 409,
  "detail": "The instructor already has a session in this interval.",
  "instance": "/api/v1/schedule-rules/01990d75-1e49-7d61-a29d-1d73501ac033",
  "code": "SCHEDULE_INSTRUCTOR_CONFLICT",
  "traceId": "8af61c2a",
  "errors": []
}
```

خطاهای Validation در `errors` شامل `field`، `code` و `message` هستند. Client نباید منطق خود را براساس متن ترجمه‌شده `message` بسازد؛ `code` قرارداد پایدار است.

## Status Codeها

| وضعیت | کاربرد |
|---:|---|
| 200 | خواندن یا تغییر موفق |
| 201 | ایجاد Resource |
| 204 | عملیات موفق بدون بدنه |
| 400 | ورودی نامعتبر یا قاعده عمومی شکسته |
| 401 | احراز هویت نشده |
| 403 | Permission، Scope یا رابطه لازم وجود ندارد |
| 404 | Resource در Tenant فعال یافت نشد |
| 409 | تعارض وضعیت، نسخه یا زمان‌بندی |
| 422 | درخواست معتبر ولی از نظر قاعده دامنه اجراشدنی نیست |

برای جلوگیری از افشای داده، Resource متعلق به Tenant دیگر مانند Resource ناموجود پاسخ داده می‌شود.

## زمان و تاریخ

- Instant با ISO-8601 و offset ارسال می‌شود؛ مثال `2026-09-15T08:30:00Z`.
- تاریخ محلی به شکل ISO Gregorian `YYYY-MM-DD` در API است.
- تقویم شمسی مسئولیت نمایش و ورودی رابط کاربری است و قبل از ارسال به قرارداد API تبدیل می‌شود.
- Rule زمان‌بندی همیشه `timeZone` از شناسه IANA دارد؛ مانند `Asia/Tehran`.

## عملیات تغییردهنده حساس

- Callback پرداخت و عملیات قابل تکرار از `Idempotency-Key` پشتیبانی می‌کنند.
- برای تغییر موجودیت‌های در معرض ویرایش هم‌زمان از version و Optimistic Lock استفاده می‌شود.
- Actionهای دامنه‌ای با Endpoint صریح بیان می‌شوند:

```text
POST /api/v1/enrollments/{id}/cancel
POST /api/v1/class-sessions/{id}/reschedule
POST /api/v1/assessments/{id}/publish
POST /api/v1/results/{id}/publish
```

تغییر وضعیت حساس با PATCH عمومی انجام نمی‌شود.

## جست‌وجو و مرتب‌سازی

- فیلترهای ساده query parameter هستند.
- مرتب‌سازی با `sort=field,asc` انجام می‌شود.
- فیلدهای قابل فیلتر و مرتب‌سازی برای هر Endpoint whitelist می‌شوند.
- جست‌وجوی متنی باید نرمال‌سازی حروف فارسی و عربی را در نظر بگیرد.

## OpenAPI

- قرارداد OpenAPI بخشی از خروجی build است.
- نام operationها پایدار و یکتا است تا Client قابل تولید باشد.
- Example خطا و موفقیت برای Endpointهای اصلی ثبت می‌شود.
- تغییر ناسازگار قرارداد در CI شناسایی خواهد شد.
