# Walkthrough - Quotes Carousel & Horizontal Scrolling Fixes

We have successfully converted the static Durood card into a dynamic ViewPager2 Quotes Carousel and fixed the horizontal scrolling lists for "Continue Reading" and "Your Bookmarks".

## Changes Made

### 1. Dynamic Quotes Carousel (`QuotesCarouselAdapter.kt`, `QuoteItem.kt`, `item_quote_card.xml`, `layout_quotes_carousel.xml`)
- Replaced the static Durood card with a fully functional ViewPager2 carousel (`viewPagerQuotes`) displaying rotating Islamic quotes, Duas, and sayings.
- Configured a smooth 4-second auto-scroll handler that pauses on user touch / swipe and automatically resumes after.
- Added a `TabLayout` dots indicator below the ViewPager2 via `TabLayoutMediator`.

### 2. Horizontal Scrolling Fix for "Continue Reading" & "Your Bookmarks" (`HomeActivity.kt`)
- Updated `RecentReadList()` in `HomeActivity.kt` to use fixed item widths (`220dp`) with `wrap_content` height, enabling smooth Left-to-Right horizontal scrolling.

## Verification Results

### Automated Tests
- Successfully compiled and built the project (`gradle_build("app:assembleDebug")`) with zero errors.
