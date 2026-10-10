# Dashboard Cleanup & Enhanced Carousel Banner

Clean up the Home dashboard by removing the Quick Access section and placing an auto-sliding ViewPager2 Carousel Banner at the top (right below the App Header/Toolbar). Connect it to Firestore collection `Announcements` (querying `is_active == true`), complete with TabLayout / dots indicator, depth page transformer, auto-scroll lifecycle handling, and clean card styling.

## User Review Required

> [!NOTE]
> - Quick Access grid cards and horizontal chips will be removed from `HomeActivity.kt`.
> - Carousel Banner will be placed at the top of the dashboard.
> - Firestore collection query will check `Announcements` (and fallback to `board_announcements`).
> - Auto-scroll handler will pause on stop / resume on start.

## Proposed Changes

### Layouts
#### [NEW / MODIFY] [item_announcement_card.xml](file:///D:/app%20zip/StudyPoint/app/src/main/res/layout/item_announcement_card.xml)
- Update card layout with a soft gradient background, 16dp rounded corners, 4dp elevation, category chip at top-left, bold title/quote text (16sp), and description text (14sp).

#### [NEW / MODIFY] [layout_announcement_slider.xml](file:///D:/app%20zip/StudyPoint/app/src/main/res/layout/layout_announcement_slider.xml)
- Update slider layout to include ViewPager2 and a `TabLayout` (dots indicator) positioned below it.

### Adapters & Logic
#### [MODIFY] [AnnouncementAdapter.kt](file:///D:/app%20zip/StudyPoint/app/src/main/kotlin/com/example/studypoint/AnnouncementAdapter.kt)
- Enhance adapter to bind announcement data cleanly.

#### [MODIFY] [HomeActivity.kt](file:///D:/app%20zip/StudyPoint/app/src/main/kotlin/com/example/studypoint/HomeActivity.kt)
- Remove `QuickAccessGrid` and `QuickAccessChips`.
- Move `AnnouncementCarousel` to the top (right below HeaderSection & SearchBar).
- Implement Firestore real-time snapshot listener on collection `Announcements` (where `is_active == true`), 4-second auto-scroll loop with lifecycle pause/resume, and depth PageTransformer.

## Verification Plan

### Automated Tests
- Build project using `gradle_build("app:assembleDebug")` to ensure compilation succeeds.

### Manual Verification
- Verify Quick Access section is completely removed.
- Verify Carousel Banner sits at the top, auto-scrolls every 4 seconds, supports manual swiping, and shows dots indicator.
