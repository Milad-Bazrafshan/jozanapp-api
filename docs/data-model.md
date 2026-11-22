# مدل داده سطح بالا

این مدل برای تعیین مالکیت و ارتباط‌هاست و جایگزین طراحی Migrationهای هر ماژول نیست.

## نمای کلی

```mermaid
erDiagram
    TENANT ||--|| EDUCATIONAL_INSTITUTION : owns
    EDUCATIONAL_INSTITUTION ||--o{ BRANCH : contains
    USER_ACCOUNT ||--o{ TENANT_MEMBERSHIP : joins
    TENANT ||--o{ TENANT_MEMBERSHIP : grants
    PERSON ||--o| TENANT_MEMBERSHIP : represents
    PERSON ||--o| STUDENT_PROFILE : may_be
    PERSON ||--o| INSTRUCTOR_PROFILE : may_be
    PERSON ||--o| GUARDIAN_PROFILE : may_be
    GUARDIAN_PROFILE ||--o{ STUDENT_GUARDIAN : relates
    STUDENT_PROFILE ||--o{ STUDENT_GUARDIAN : protected_by

    TENANT ||--o{ ACADEMIC_PERIOD : defines
    TENANT ||--o{ PROGRAM : defines
    PROGRAM ||--o{ PROGRAM_COURSE : includes
    COURSE ||--o{ PROGRAM_COURSE : appears_in
    COURSE ||--o{ COURSE_OFFERING : offered_as
    ACADEMIC_PERIOD ||--o{ COURSE_OFFERING : during
    BRANCH ||--o{ COURSE_OFFERING : hosts
    COURSE_OFFERING ||--o{ CLASS_SECTION : divided_into
    INSTRUCTOR_PROFILE ||--o{ CLASS_INSTRUCTOR : assigned
    CLASS_SECTION ||--o{ CLASS_INSTRUCTOR : taught_by

    STUDENT_PROFILE ||--o{ ENROLLMENT : has
    CLASS_SECTION ||--o{ ENROLLMENT : contains
    CLASS_SECTION ||--|| CLASS_SCHEDULE : scheduled_by
    CLASS_SCHEDULE ||--o{ SCHEDULE_RULE : consists_of
    SCHEDULE_RULE ||--o{ CLASS_SESSION : generates
    CLASS_SESSION ||--o{ SESSION_INSTRUCTOR : uses
    CLASS_SESSION ||--o{ ATTENDANCE_RECORD : records
    STUDENT_PROFILE ||--o{ ATTENDANCE_RECORD : receives

    ASSESSMENT ||--o{ ASSESSMENT_QUESTION : contains
    QUESTION_VERSION ||--o{ ASSESSMENT_QUESTION : snapshots
    ASSESSMENT ||--o{ ASSESSMENT_PARTICIPANT : publishes_to
    STUDENT_PROFILE ||--o{ ASSESSMENT_PARTICIPANT : participates
    ASSESSMENT_PARTICIPANT ||--o{ ASSESSMENT_ATTEMPT : attempts
    ASSESSMENT_ATTEMPT ||--o{ STUDENT_RESPONSE : answers
    ASSESSMENT_PARTICIPANT ||--o| ASSESSMENT_RESULT : receives

    ENROLLMENT ||--o{ CHARGE : incurs
    INVOICE ||--o{ INVOICE_ITEM : contains
    CHARGE ||--o{ INVOICE_ITEM : billed_as
    PAYMENT ||--o{ PAYMENT_ALLOCATION : allocates
    CHARGE ||--o{ PAYMENT_ALLOCATION : settles
```

## Aggregateهای پیشنهادی

| Aggregate Root | موجودیت‌های داخلی مهم | Invariant اصلی |
|---|---|---|
| Tenant | Institution، تنظیمات | مرز مالکیت تغییر نمی‌کند |
| UserAccount | Credential، Session، ExternalIdentity | شناسه ورود و Provider یکتا است |
| Person | Profileها، شناسه‌ها | داده شخص داخل Tenant یکتا و خصوصی است |
| Program | ProgramCourseها | کد Program در Tenant یکتا است |
| CourseOffering | ClassSectionها | بازه و شعبه معتبر هستند |
| Enrollment | StatusHistory | یک عضویت فعال برای دانش‌آموز و کلاس |
| ClassSchedule | ScheduleRuleها | بازه‌ها معتبر و تداخل کنترل‌شده است |
| ClassSession | Instructor، Location، Attendance | جلسه نهایی‌شده خودکار بازنویسی نمی‌شود |
| Question | QuestionVersionها | نسخه منتشرشده تغییرناپذیر است |
| Assessment | Section، Question، Participant، Policy | انتشار Snapshot قابل بازتولید می‌سازد |
| AssessmentAttempt | Responseها، Eventها | محدودیت زمان و تعداد تلاش رعایت می‌شود |
| AssessmentResult | GradeHistory، Publication | نمره منتشرشده فقط با اصلاح ثبت‌شده تغییر می‌کند |
| Invoice | InvoiceItemها | جمع اقلام با مبلغ فاکتور سازگار است |
| Payment | Allocationها | مجموع تخصیص از پرداخت موفق بیشتر نیست |

## وضعیت‌های اصلی

### EnrollmentStatus

```text
PENDING → ACTIVE → COMPLETED
             ├── CANCELLED
             ├── DROPPED
             └── TRANSFERRED
```

### SessionStatus

```text
PLANNED → CONFIRMED → IN_PROGRESS → COMPLETED
   ├── CANCELLED
   └── POSTPONED → RESCHEDULED
```

`RESCHEDULED` باید دارای ارجاع به جلسه جایگزین باشد.

### AssessmentStatus

```text
DRAFT → PUBLISHED → ACTIVE → CLOSED → GRADING → FINALIZED
  └── CANCELLED             └───────────────┘
```

بازگشت از `FINALIZED` فقط از طریق فرایند اصلاح کنترل‌شده ممکن است.

### AttemptStatus

```text
NOT_STARTED → IN_PROGRESS → SUBMITTED
                    ├────→ AUTO_SUBMITTED
                    └────→ EXPIRED
```

### PaymentStatus

```text
CREATED → PENDING → SUCCEEDED
             ├── FAILED
             └── CANCELLED
SUCCEEDED → PARTIALLY_REFUNDED → REFUNDED
```

## قواعد مشترک داده

- شناسه‌ها UUID هستند و شناسه تجاری جداگانه دارند.
- زمان ایجاد و تغییر و عامل تغییر برای داده‌های مهم ثبت می‌شود.
- مبلغ با نوع decimal و Currency ذخیره می‌شود؛ از float استفاده نمی‌شود.
- Instantها در UTC ذخیره و منطقه زمانی Rule جداگانه نگهداری می‌شود.
- تاریخ آموزشی محلی در صورت نیاز کنار Instant ثبت می‌شود.
- حذف عمومی Soft Delete نداریم؛ هر دامنه Archive، Cancel یا تاریخ اثر مناسب خود را تعریف می‌کند.
- داده تاریخی مانند نتیجه منتشرشده، پرداخت موفق و Audit Log حذف فیزیکی نمی‌شود.
- Constraintهای طبیعی مانند کد دانش‌آموز یا شماره فاکتور همراه `tenant_id` تعریف می‌شوند.

## مدل Multi-Tenant

رابطه منطقی تمام Aggregateهای تجاری به شکل زیر است:

```text
tenant_id + entity_id
```

Foreign Keyهای حساس باید تا حد امکان Tenant را نیز در Constraint پوشش دهند تا اتصال تصادفی دو Tenant فقط در لایه برنامه کنترل نشود. طراحی دقیق Composite Constraint هنگام ساخت Migration هر ماژول انجام می‌شود.

## موارد باز مدل

- `Open`: نیاز قطعی به Student Group مستقل از Class Section برای مدارس.
- `Open`: ساختار طبقه‌بندی Program برای پایه، رشته، سطح زبان و مهارت.
- `Open`: سیاست شماره‌گذاری دانش‌آموز، فاکتور و رسید در سطح Tenant یا Branch.
- `Open`: حمایت از ماه شمسی در قواعد تکرار ماهانه؛ تکرار هفتگی بدون این تصمیم قابل اجرا است.
