# فاز ۸: اجرای آزمون، تصحیح و نتیجه

## خروجی

- شروع امن و idempotent تلاش آزمون برای شرکت‌کننده واجد شرایط
- محاسبه مهلت هر تلاش با رعایت مدت آزمون و زمان بسته‌شدن آن
- ذخیره مجدد پاسخ هر سؤال با قید یکتای تلاش و سؤال
- تحویل دستی و تحویل خودکار تلاش‌های منقضی‌شده با Scheduled Job
- تصحیح خودکار تک‌گزینه‌ای، چندگزینه‌ای، صحیح/غلط و پاسخ کوتاه
- صف تصحیح پاسخ تشریحی، کنترل سقف نمره و ثبت دلیل تغییر
- تجمیع نمره و وضعیت‌های `PENDING_GRADING`، `READY` و `PUBLISHED`
- نمایش نتیجه به دانش‌آموز فقط پس از انتشار
- ثبت رویدادهای تلاش، تاریخچه تغییر نمره و Audit رخدادهای مدیریتی

## سیاست تلاش و نتیجه

شروع دوباره یک آزمون دارای تلاش فعال همان تلاش را برمی‌گرداند. هر تلاش در زمان
`min(startedAt + duration, assessment.closesAt)` پایان می‌یابد. با تحویل هر تلاش، همان
تلاش به‌عنوان تلاش انتخاب‌شده نتیجه قرار می‌گیرد؛ بنابراین در آزمون چندتلاشه، آخرین تلاش
تحویل‌شده مبنای نتیجه است.

پاسخ تشریحی خالی نمره صفر می‌گیرد و وارد صف تصحیح نمی‌شود. نتیجه دارای پاسخ تشریحی
پاسخ‌داده‌شده تا ثبت همه نمره‌های دستی قابل انتشار نیست. اصلاح نمره با دلیل در
`grade_history` نگهداری می‌شود.

## Permissionها

- `GRADING_READ`
- `GRADING_WRITE`
- `RESULT_PUBLISH`

Initializer هنگام بالا آمدن برنامه این مجوزها را نیز می‌سازد و به نقش سیستمی Admin می‌دهد.

## APIهای دانش‌آموز

- `POST /api/v1/assessment/assessments/{assessmentId}/attempts`
- `GET /api/v1/assessment/attempts/{attemptId}`
- `PUT /api/v1/assessment/attempts/{attemptId}/responses/{questionId}`
- `POST /api/v1/assessment/attempts/{attemptId}/submit`
- `GET /api/v1/assessment/me/results`

## APIهای تصحیح

- `GET /api/v1/assessment/assessments/{assessmentId}/results`
- `PUT /api/v1/assessment/responses/{responseId}/grade`
- `POST /api/v1/assessment/results/{resultId}/publish`
