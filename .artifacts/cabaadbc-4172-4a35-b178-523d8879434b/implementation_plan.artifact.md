# Implementation Plan - Universal 4-Category Study Material & Past Papers Year Selection

## Problem Description
The user wants to standardize all subjects across all classes (9th, 10th, 11th, 12th) to have exactly 4 categories:
1. `"Notes"` (Direct PDF list/link)
2. `"Book"` (Direct PDF link)
3. `"Keybook"` (Direct PDF link)
4. `"Past Papers"` (Map/list containing 5 Year-Wise entries: `"2022"`, `"2023"`, `"2024"`, `"2025"`, `"2026"`).

Navigation behavior updates:
- Clicking `"Notes"`, `"Book"`, or `"Keybook"` directly opens the PDF Viewer Activity (`PdfViewActivity`).
- Clicking `"Past Papers"` opens a Year Selection screen showing buttons for years 2022 to 2026.
- Clicking any year button opens the corresponding Past Paper PDF.

## User Review Required
> [!IMPORTANT]
> - Standardizing `pdfLinks` data structure for classes 9, 10, 11, and 12 across all major subjects (Physics, Chemistry, Mathematics, Biology, Computer Science, English) with placeholder links.
> - Updating `MaterialSelectionActivity.kt` and `NotesActivity.kt` click handling logic to route Book/Notes/Keybook directly to PDF viewer, and Past Papers to year selection buttons (2022–2026).

## Proposed Changes

### Data & Activity Logic
#### [MODIFY] [NotesActivity.kt](file:///D:/app%20zip/StudyPoint/app/src/main/kotlin/com/example/studypoint/NotesActivity.kt)
- Define a comprehensive `pdfLinks` map for classes `"9th"`, `"10th"`, `"11th"`, and `"12th"` containing all core subjects. Each subject will have `"Notes"` (`List<String>`), `"Book"` (`String`), `"Keybook"` (`String`), and `"Past Papers"` (a map or list of years `"2022"` to `"2026"` with PDF URLs).
- Update click handling:
  - If `material` is `"Book"`, `"Keybook"`, or `"Notes"` (with 1 item), open `PdfViewActivity` directly.
  - If `material` is `"Past Papers"`, populate `btnCh1` through `btnCh5` with year labels (`"2022"`, `"2023"`, `"2024"`, `"2025"`, `"2026"`), and clicking any year opens that year's past paper PDF in `PdfViewActivity`.
  - If `material` is `"Notes"` with multiple parts, list them as `"Notes Part 1"`, `"Notes Part 2"`, etc.

#### [MODIFY] [StudyMaterialActivity.kt](file:///D:/app%20zip/StudyPoint/app/src/main/kotlin/com/example/studypoint/StudyMaterialActivity.kt)
- Align `StudyMaterialActivity.kt`'s `pdfLinks` structure and helper methods to match `NotesActivity.kt`.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure compilation succeeds without errors.

### Manual Verification
- Test navigation from Class -> Subject -> Material Selection:
  - Clicking Book, Notes, Keybook opens PDF viewer directly.
  - Clicking Past Papers shows the 2022-2026 year selection buttons, and clicking a year opens the past paper PDF.
