# فاز ۳: اشخاص و ساختار آموزشی

## خروجی قابل استفاده

- ثبت مستقل Person و Profileهای دانش‌آموز، استاد و ولی
- کد مستقل دانش‌آموز و استاد در هر Tenant
- ساخت حساب ورود اختیاری هنگام ثبت Profile
- رابطه تاریخ‌دار چندبه‌چند ولی و دانش‌آموز
- تعیین ولی اصلی و دسترسی آموزشی، مالی، اعلان و تحویل دانش‌آموز
- تعریف بازه آموزشی با تاریخ شروع و پایان
- تعریف برنامه متناسب با مدرسه، زبان، کنکور، فنی‌وحرفه‌ای یا نوع عمومی
- تعریف Course مستقل از زمان، استاد و کلاس
- اتصال Course به Academic Program همراه ترتیب و الزامی‌بودن
- نمایش Profile شخص واردشده و فرزندان مجاز در پنل کاربری
- مدیریت همه موارد بالا در پنل مدیریت

## مدل اصلی

```text
Person
├── StudentProfile
├── InstructorProfile
└── GuardianProfile
    └── StudentGuardian ── StudentProfile

AcademicProgram ── ProgramCourse ── Course
AcademicPeriod
```

Course فقط تعریف محتوایی درس است. شعبه، بازه آموزشی، ظرفیت، استاد و زمان‌بندی در
Course Offering و Class Section فاز بعد قرار می‌گیرند.

## APIها

| مسیر | کاربرد |
|---|---|
| `GET /api/v1/people` | فهرست اشخاص و روابط برای مدیر |
| `GET /api/v1/people/me` | Profileهای شخص واردشده |
| `POST /api/v1/people/students` | ثبت دانش‌آموز |
| `POST /api/v1/people/instructors` | ثبت استاد |
| `POST /api/v1/people/guardians` | ثبت ولی |
| `POST /api/v1/people/guardian-links` | اتصال ولی و دانش‌آموز |
| `GET /api/v1/academic` | کاتالوگ آموزشی |
| `POST /api/v1/academic/periods` | تعریف بازه |
| `POST /api/v1/academic/programs` | تعریف برنامه |
| `POST /api/v1/academic/courses` | تعریف درس |
| `POST /api/v1/academic/program-courses` | اتصال درس به برنامه |

همه عملیات مدیریتی به Permission سطح Tenant نیاز دارند. Constraintهای دیتابیس نیز
ارتباط Person، Profile، Program و Course را در همان Tenant نگه می‌دارند.
