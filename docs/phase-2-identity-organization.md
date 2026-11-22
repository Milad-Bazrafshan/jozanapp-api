# فاز ۲: هویت، سازمان و دسترسی

## خروجی قابل استفاده

- ورود کاربر با `tenantSlug`، ایمیل و رمز عبور
- Access Token مبتنی بر JWT با عمر کوتاه
- Refresh Token تصادفی، ذخیره‌شده به‌صورت SHA-256 و چرخش در هر بار استفاده
- خروج از حساب و بازیابی رمز عبور
- الزام `X-Tenant-Id` و تطبیق آن با Tenant داخل JWT
- مدیریت مؤسسه، شعبه، نقش، مجوز، کاربر و دسترسی در سطح Tenant یا Branch
- ثبت Audit برای ورود و تغییرات مدیریتی
- Bootstrap اختیاری مدیر، مجموعه، شعبه اصلی و نقش Admin
- صفحه ورود و پنل مدیریت در `admin-front`
- صفحه ورود کاربر در `client-front`

## اجرای محلی

در Backend فایل `.env.example` را به `.env` کپی کنید و سپس:

```powershell
docker compose up --build
```

ورود پیش‌فرض محیط توسعه:

| فیلد | مقدار |
|---|---|
| شناسه مجموعه | `demo-school` |
| ایمیل | `admin@example.com` |
| رمز | `change-me-now` |

این مقادیر باید خارج از محیط توسعه تغییر کنند. نمایش Token بازیابی رمز فقط با `EXPOSE_PASSWORD_RESET_TOKEN=true` فعال می‌شود.

## APIهای اصلی

| مسیر | کاربرد |
|---|---|
| `POST /api/v1/auth/login` | ورود |
| `POST /api/v1/auth/refresh` | چرخش Refresh Token |
| `POST /api/v1/auth/logout` | خروج |
| `GET /api/v1/auth/me` | کاربر جاری |
| `GET /api/v1/organization/current` | مجموعه جاری |
| `GET/POST /api/v1/organization/branches` | شعب |
| `GET/POST /api/v1/organization/roles` | نقش‌ها |
| `GET/POST /api/v1/organization/users` | کاربران |

## مرز Orix Wallet

جدول `external_identity` فقط محل اتصال آیندهٔ حساب سراسری به شناسه کیف پول است. این فاز هیچ وابستگی اجرایی به Orix Bank ندارد.
