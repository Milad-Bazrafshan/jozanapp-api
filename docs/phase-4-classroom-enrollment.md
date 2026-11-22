# فاز ۴: ارائه درس، کلاس و ثبت‌نام

## خروجی قابل استفاده

- ایجاد Course Offering برای ترکیب درس، بازه آموزشی و شعبه
- ایجاد چند Class Section برای هر Offering
- ظرفیت و شیوه برگزاری مستقل هر کلاس
- تخصیص چند استاد با نقش و بازه اثر
- استاد اصلی، دستیار، جایگزین، مهمان و ممتحن
- ثبت‌نام تاریخ‌دار دانش‌آموز در کلاس
- جلوگیری از عضویت جاری تکراری
- قفل پایگاه داده هنگام کنترل ظرفیت
- قرارگیری اختیاری در لیست انتظار
- انصراف و انتقال اتمیک میان کلاس‌ها
- تاریخچه تغییر وضعیت ثبت‌نام
- نمایش کلاس‌های دانش‌آموز در پنل کاربری

## مرز مدل

```text
Course
└── CourseOffering (AcademicPeriod + Branch)
    └── ClassSection
        ├── ClassInstructor ── InstructorProfile
        └── Enrollment ── StudentProfile
```

`ClassSection` شامل استاد، روز هفته یا ساعت شروع نیست. برنامه تکرارشونده و جلسه واقعی
در ماژول Scheduling فاز بعد ساخته می‌شوند.

## APIهای اصلی

| مسیر | کاربرد |
|---|---|
| `GET /api/v1/classroom` | ارائه‌ها، کلاس‌ها و استادان |
| `POST /api/v1/classroom/offerings` | ایجاد ارائه درس |
| `POST /api/v1/classroom/sections` | ایجاد کلاس |
| `POST /api/v1/classroom/instructors` | تخصیص استاد |
| `GET /api/v1/enrollments` | ثبت‌نام‌های مجموعه |
| `GET /api/v1/enrollments/me` | کلاس‌های دانش‌آموز جاری |
| `POST /api/v1/enrollments` | ثبت‌نام یا لیست انتظار |
| `POST /api/v1/enrollments/{id}/cancel` | انصراف |
| `POST /api/v1/enrollments/{id}/transfer` | انتقال |
