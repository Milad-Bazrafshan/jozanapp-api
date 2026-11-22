# فاز ۷: بانک سؤال و آزمون‌ساز

## خروجی

- بانک سؤال با انواع تک‌گزینه‌ای، چندگزینه‌ای، صحیح/غلط، پاسخ کوتاه و تشریحی
- ایجاد نسخه جدید بدون تغییر نسخه‌های قبلی
- تعریف آزمون، کوییز و تکلیف زمان‌دار
- ساخت بخش‌های مرتب آزمون
- اتصال یک `QuestionVersion` مشخص با ترتیب و امتیاز
- هدف‌گیری یک یا چند کلاس با کنترل دسترسی استاد و شعبه
- اعتبارسنجی سؤال، بازه زمانی، امتیاز، هدف و شرکت‌کننده پیش از انتشار
- Snapshot شرکت‌کنندگان از Enrollmentهای فعال هنگام انتشار
- قفل‌شدن ساختار آزمون پس از انتشار
- نمایش آزمون‌های منتشرشده در پنل دانش‌آموز

## مرز انتشار

ویرایش سؤال با ساخت `QuestionVersion` جدید انجام می‌شود. `AssessmentQuestion` به شناسه نسخه
سؤال متصل است؛ بنابراین تغییرات آینده بانک سؤال روی آزمون منتشرشده اثر ندارد. شرکت‌کنندگان
نیز هنگام انتشار در `AssessmentParticipant` همراه نام و کد دانش‌آموز Snapshot می‌شوند.

## Permissionها

- `QUESTION_READ`
- `QUESTION_WRITE`
- `ASSESSMENT_READ`
- `ASSESSMENT_WRITE`
- `ASSESSMENT_PUBLISH`

## APIهای اصلی

- `GET|POST /api/v1/assessment/questions`
- `POST /api/v1/assessment/questions/{id}/versions`
- `GET|POST /api/v1/assessment/assessments`
- `POST /api/v1/assessment/assessments/{id}/sections`
- `POST /api/v1/assessment/assessments/{id}/questions`
- `POST /api/v1/assessment/assessments/{id}/targets`
- `POST /api/v1/assessment/assessments/{id}/publish`
- `GET /api/v1/assessment/me`

شروع Attempt، ذخیره پاسخ، تحویل و تصحیح در فاز اجرای آزمون پیاده می‌شود.
