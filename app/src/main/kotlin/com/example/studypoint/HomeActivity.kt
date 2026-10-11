package com.example.studypoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.studypoint.practicetest.QuizSelectionActivity
import com.example.studypoint.practicetest.BookmarksActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            StudyPointTheme {
                HomeScreen(
                    userName = auth.currentUser?.displayName?.ifBlank { "Student" } ?: "Student",
                    userEmail = auth.currentUser?.email ?: "",
                    onLogout = { logout() },
                    onFeedback = { sendFeedback() },
                    onWhatsApp = { openWhatsApp() },
                    onShare = { shareApp() },
                    onClassClick = { grade ->
                        val intent = Intent(this, SubjectsActivity::class.java)
                        intent.putExtra("grade", grade)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("alsamadacadmey6844@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "StudyPoint Feedback")
        }
        startActivity(intent)
    }

    private fun openWhatsApp() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Vah4pV3Au3aVHGzJL805")))
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Download StudyPoint App for PCTB Notes, Past Papers & AI Tutor! https://studypoint.pk")
        }
        startActivity(Intent.createChooser(intent, "Share StudyPoint"))
    }
}

// Data Models
data class AiChatMessage(val isUser: Boolean, val text: String, val time: String)
data class QuickActionItem(val title: String, val subtitle: String, val icon: ImageVector, val containerColor: Color, val iconColor: Color, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onFeedback: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit,
    onClassClick: (String) -> Unit
) {
    val context = LocalContext.current
    val recentItems = remember { RecentReadManager.getRecent(context) }
    val bookmarks = remember { RecentReadManager.getBookmarks(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Navigation and interactive sheets state
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAiChatSheet by remember { mutableStateOf(false) }
    var appUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Dynamic YouTube Feed State
    var youTubeVideos by remember { mutableStateOf<List<YouTubeVideo>>(YouTubeRepository.sampleLectures) }
    var isYouTubeLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fetched = YouTubeRepository.fetchLatestVideos()
        youTubeVideos = fetched
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color(0xFFF8FAFC)
            ) {
                DrawerHeader(userName, userEmail)
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home Dashboard") },
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.School, null) },
                        label = { Text("All Subjects & Classes") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, GradeSelectionActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Quiz, null) },
                        label = { Text("Practice Tests / MCQs") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, QuizSelectionActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Bookmark, null, tint = Color(0xFFFFB300)) },
                        label = { Text("Saved Questions") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, BookmarksActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7C3AED)) },
                        label = { Text("Ask AI Assistant") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAiChatSheet = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Public, null) },
                        label = { Text("Punjab Board Portals") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            context.startActivity(Intent(context, BoardsActivity::class.java))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                    // Expandable Classes (PCTB Curriculum)
                    ExpandableClassItem("Class 9th", listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Islamiyat", "Tarjuma-tul-Quran"), onClassClick)
                    ExpandableClassItem("Class 10th", listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Pak Studies", "Tarjuma-tul-Quran"), onClassClick)
                    ExpandableClassItem("Class 11th (1st Year)", listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Islamiyat", "Tarjuma-tul-Quran"), onClassClick)
                    ExpandableClassItem("Class 12th (2nd Year)", listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Pak Studies", "Tarjuma-tul-Quran"), onClassClick)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Share, null) },
                        label = { Text("Share StudyPoint App") },
                        selected = false,
                        onClick = onShare,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF10B981)) },
                        label = { Text("Join WhatsApp Channel") },
                        selected = false,
                        onClick = onWhatsApp,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFEF4444)) },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = onLogout,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            // AI Assistant floating button (small, like common chat FABs)
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
                            ),
                            CircleShape
                        )
                        .clickable { showAiChatSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Ask AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            // 6. Fixed Bottom Navigation Bar
            bottomBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        // 1. Home Tab
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4F46E5),
                                selectedTextColor = Color(0xFF4F46E5),
                                indicatorColor = Color(0xFFEEF2FF),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )

                        // 2. Subjects Tab
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                context.startActivity(Intent(context, GradeSelectionActivity::class.java))
                            },
                            icon = { Icon(Icons.Default.School, contentDescription = "Subjects") },
                            label = { Text("Subjects", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4F46E5),
                                selectedTextColor = Color(0xFF4F46E5),
                                indicatorColor = Color(0xFFEEF2FF),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )

                        // 3. AI Chat Tab
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                showAiChatSheet = true
                            },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Chat") },
                            label = { Text("AI Chat", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF7C3AED),
                                selectedTextColor = Color(0xFF7C3AED),
                                indicatorColor = Color(0xFFF5F3FF),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )

                        // 4. MCQs Tab
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = {
                                selectedTab = 3
                                context.startActivity(Intent(context, QuizSelectionActivity::class.java))
                            },
                            icon = { Icon(Icons.Default.Quiz, contentDescription = "MCQs") },
                            label = { Text("MCQs", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF10B981),
                                selectedTextColor = Color(0xFF10B981),
                                indicatorColor = Color(0xFFECFDF5),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )

                        // 5. Profile / More Tab
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = {
                                selectedTab = 4
                                showProfileDialog = true
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4F46E5),
                                selectedTextColor = Color(0xFF4F46E5),
                                indicatorColor = Color(0xFFEEF2FF),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8FAFC))
                    .verticalScroll(scrollState)
            ) {
                // 1. Top Greeting Header
                TopGreetingHeader(
                    userName = userName,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSearchClick = { isSearchExpanded = !isSearchExpanded },
                    onRefreshClick = {
                        scope.launch {
                            isYouTubeLoading = true
                            youTubeVideos = YouTubeRepository.fetchLatestVideos()
                            isYouTubeLoading = false
                        }
                        Toast.makeText(context, "Dashboard & live lectures refreshed! 🔥", Toast.LENGTH_SHORT).show()
                    },
                    onNotificationClick = { showNotificationDialog = true }
                )

                // Expandable Search Bar
                AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        placeholder = { Text("Search Class 9/10/11/12, Physics, Pak Studies, MCQs...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4F46E5)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true
                    )
                }

                // If Search Active: Display Results
                if (searchQuery.isNotEmpty()) {
                    SectionTitle("Search Results")
                    SearchResultsList(searchQuery, onClassClick)
                } else {
                    // Date / Time / Hijri header bar
                    DateTimeHeader()

                    // 2. Daily Tracker / Status Card
                    DailyTrackerCard(
                        onStartChallenge = {
                            context.startActivity(Intent(context, QuizSelectionActivity::class.java))
                        }
                    )

                    // 3. Quick Action Feature Grid (Textbooks, Notes, MCQs, AI Tutor, Past Papers)
                    SectionTitle("Quick Actions")
                    QuickActionsGrid(
                        onTextbooksClick = {
                            val intent = Intent(context, GradeSelectionActivity::class.java)
                            context.startActivity(intent)
                        },
                        onNotesClick = {
                            val intent = Intent(context, GradeSelectionActivity::class.java)
                            context.startActivity(intent)
                        },
                        onMcqsClick = {
                            context.startActivity(Intent(context, QuizSelectionActivity::class.java))
                        },
                        onAiTutorClick = {
                            showAiChatSheet = true
                        },
                        onPastPapersClick = {
                            val intent = Intent(context, GradeSelectionActivity::class.java)
                            context.startActivity(intent)
                        }
                    )



                    // Inspirational Quotes Carousel
                    SectionTitle("Daily Educational Inspiration")
                    QuotesCarousel()

                    // Dynamic Announcements Banner
                    AnnouncementCarousel()

                    // Classes Curriculum Hub (9th, 10th, 11th, 12th)
                    SectionTitle("Classes (PCTB Curriculum 2026)")
                    ClassCategoriesGrid(onClassClick)

                    // Continue Reading (if any)
                    if (recentItems.isNotEmpty()) {
                        SectionTitle("Continue Reading")
                        RecentReadList(recentItems) { item ->
                            val intent = Intent(context, PdfViewActivity::class.java).apply {
                                putExtra("pdfUrl", item.fileName)
                                putExtra("grade", item.grade)
                                putExtra("subject", item.subject)
                                putExtra("title", item.title)
                            }
                            context.startActivity(intent)
                        }
                    }

                    // Bookmarks (if any)
                    if (bookmarks.isNotEmpty()) {
                        SectionTitle("Your Saved Bookmarks")
                        RecentReadList(bookmarks) { item ->
                            val intent = Intent(context, PdfViewActivity::class.java).apply {
                                putExtra("pdfUrl", item.fileName)
                                putExtra("grade", item.grade)
                                putExtra("subject", item.subject)
                                putExtra("title", item.title)
                            }
                            context.startActivity(intent)
                        }
                    }

                    // Punjab Educational Boards Section
                    SectionTitle("Punjab Educational Boards")
                    BoardResultsCard {
                        context.startActivity(Intent(context, BoardsActivity::class.java))
                    }

                    // YouTube Live Lectures Carousel (Main Dashboard Only)
                    SectionTitle("Latest Educational Videos / Channel Lectures")
                    YouTubeVideosSection(
                        videos = youTubeVideos,
                        isLoading = isYouTubeLoading,
                        onVideoClick = { video ->
                            YouTubePlayerActivity.playDirectOrApp(context, video.id, video.title)
                        },
                        onViewAllClick = {
                            SocialMediaManager.openYouTubeChannel(context)
                        }
                    )

                    // Connect with Us & Social Hub (Main Dashboard Only)
                    SectionTitle("Connect with Us & Social Hub")
                    SocialMediaHubSection(
                        onFacebookClick = { SocialMediaManager.openFacebook(context) },
                        onWhatsAppClick = { SocialMediaManager.openWhatsApp(context) },
                        onYouTubeClick = { SocialMediaManager.openYouTubeChannel(context) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // In-app update check (Firestore: app_config/android_update)
    LaunchedEffect(Unit) {
        checkForAppUpdate(context) { info -> appUpdateInfo = info }
    }

    // Update available dialog
    val update = appUpdateInfo
    if (update != null) {
        AppUpdateDialog(info = update, onDismiss = { appUpdateInfo = null })
    }

    // AI Assistant Interactive BottomSheet
    if (showAiChatSheet) {
        AiChatBottomSheet(
            onDismiss = { showAiChatSheet = false }
        )
    }

    // Notifications Dialog
    if (showNotificationDialog) {
        NotificationsDialog(
            onDismiss = { showNotificationDialog = false }
        )
    }

    // Profile Dialog
    if (showProfileDialog) {
        ProfileDialog(
            userName = userName,
            userEmail = userEmail,
            onDismiss = { showProfileDialog = false },
            onLogout = onLogout,
            onShare = onShare,
            onWhatsApp = onWhatsApp,
            onFeedback = onFeedback
        )
    }
}

// ==========================================
// 1. TOP GREETING HEADER COMPONENT
// ==========================================
@Composable
fun TopGreetingHeader(
    userName: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clickable Profile/Drawer Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        )
                    )
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Greeting & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello, $userName! 👋",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Let's study smart today 🚀 • PCTB 2026",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // Right Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search Action Icon
                IconButton(onClick = onSearchClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF475569)
                    )
                }

                // Refresh Action Icon
                IconButton(onClick = onRefreshClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF475569)
                    )
                }

                // Notification Bell with Badge Count (3)
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(38.dp)) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ) {
                                Text("3", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// DATE / TIME / HIJRI HEADER BAR
// ==========================================
@Composable
fun DateTimeHeader() {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val hijriStr = remember(nowMs) {
        try {
            val hd = java.time.chrono.HijrahDate.now()
            val months = listOf(
                "", "Muharram", "Safar", "Rabi-ul-Awwal", "Rabi-us-Sani",
                "Jumada-ul-Awwal", "Jumada-us-Sani", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Zil-Qa'dah", "Zil-Hajjah"
            )
            val m = hd.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val d = hd.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val y = hd.get(java.time.temporal.ChronoField.YEAR)
            "$d ${months[m]} $y"
        } catch (_: Exception) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(timeFmt.format(java.util.Date(nowMs)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
            }
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFA7F3D0)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(dateFmt.format(java.util.Date(nowMs)), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
            }
            if (hijriStr.isNotBlank()) {
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFA7F3D0)))
                Text("🌙 $hijriStr", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF047857), maxLines = 1)
            }
        }
    }
}

// ==========================================
// 2. DAILY TRACKER / STATUS CARD COMPONENT
// ==========================================
// ==========================================
// 2. DAILY TRACKER / STATUS CARD COMPONENT (COMPACT)
// ==========================================
@Composable
fun DailyTrackerCard(onStartChallenge: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF312E81))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact streak badge
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Text(
                    text = "🔥 5-Day Streak",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = Color(0xFFFCD34D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Challenge",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "7 / 10 MCQs",
                    color = Color(0xFF34D399),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Circular arrow action button
            IconButton(
                onClick = onStartChallenge,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Start Challenge",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. QUICK ACTION FEATURE GRID COMPONENT
// ==========================================
@Composable
fun QuickActionsGrid(
    onTextbooksClick: () -> Unit,
    onNotesClick: () -> Unit,
    onMcqsClick: () -> Unit,
    onAiTutorClick: () -> Unit,
    onPastPapersClick: () -> Unit
) {
    val actions = remember {
        listOf(
            QuickActionItem("Textbooks", "درسی کتب", Icons.Default.MenuBook, Color(0xFFEFF6FF), Color(0xFF2563EB), onTextbooksClick),
            QuickActionItem("Solved Notes", "حل شدہ نوٹس", Icons.Default.Description, Color(0xFFEEF2FF), Color(0xFF4F46E5), onNotesClick),
            QuickActionItem("MCQs Practice", "کثیر الانتخابی", Icons.Default.Quiz, Color(0xFFECFDF5), Color(0xFF10B981), onMcqsClick),
            QuickActionItem("AI Tutor", "اے آئی ٹیوٹر", Icons.Default.AutoAwesome, Color(0xFFF5F3FF), Color(0xFF7C3AED), onAiTutorClick),
            QuickActionItem("Past Papers", "2022–2026", Icons.Default.Assignment, Color(0xFFFFFBEB), Color(0xFFD97706), onPastPapersClick)
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(actions) { action ->
            Card(
                modifier = Modifier
                    .width(115.dp)
                    .height(130.dp)
                    .clickable { action.onClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        action.iconColor,
                                        action.iconColor.copy(alpha = 0.55f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Text(
                        text = action.subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}



@Composable
fun StatCard(
    count: String,
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// AI ASSISTANT BOTTOM SHEET
// ==========================================
// ==========================================
// AI ASSISTANT BOTTOM SHEET
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(onDismiss: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = remember {
        mutableStateListOf(
            AiChatMessage(false, "Assalam-o-Alaikum! 🎓 I'm your StudyPoint AI Tutor. Ask me anything \u2014 concepts, formulas, definitions from any subject!", "Just now")
        )
    }

    val samplePrompts = listOf(
        "Explain Newton's 2nd Law (Physics)",
        "What is Chemical Equilibrium?",
        "What is photosynthesis?",
        "Quadratic formula in Maths"
    )

    // Auto-scroll to newest message / typing indicator
    LaunchedEffect(messages.size, isTyping) {
        val target = messages.size - 1 + if (isTyping) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target)
    }

    fun sendMessage(text: String) {
        val userQ = text.trim()
        if (userQ.isBlank() || isTyping) return
        messages.add(AiChatMessage(true, userQ, "Now"))
        prompt = ""
        isTyping = true
        scope.launch {
            val aiAnswer = try {
                AiTutorService.getAnswer(userQ)
            } catch (e: Exception) {
                "Sorry, I couldn't fetch an answer right now. Please check your internet connection and try again."
            }
            messages.add(AiChatMessage(false, aiAnswer, "Now"))
            isTyping = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient header (emerald -> navy, like splash)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF0F172A))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("StudyPoint AI Tutor", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                            Text("Smart Tutor \u2022 Powered by Web Search", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Prompt Chips
                Text("Suggested Topics:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(samplePrompts) { p ->
                        SuggestionChip(
                            onClick = { sendMessage(p) },
                            label = { Text(p, fontSize = 11.sp) }
                        )
                    }
                }

                // Chat Messages Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp, topEnd = 14.dp,
                                        bottomStart = if (msg.isUser) 14.dp else 4.dp,
                                        bottomEnd = if (msg.isUser) 4.dp else 14.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.isUser) Color(0xFF4F46E5) else Color.White
                                    ),
                                    border = if (msg.isUser) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = if (msg.isUser) Color.White else Color(0xFF0F172A),
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.time,
                                            fontSize = 9.sp,
                                            color = if (msg.isUser) Color.White.copy(0.7f) else Color(0xFF94A3B8),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        if (isTyping) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TypingIndicatorBubble()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Ask any formula, concept or question...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF059669)
                        ),
                        maxLines = 3,
                        enabled = !isTyping
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(prompt) },
                        enabled = !isTyping,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isTyping)
                                        listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                    else
                                        listOf(Color(0xFF059669), Color(0xFF4F46E5))
                                ),
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        if (isTyping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF059669), CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "AI is typing...",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
    }
}

// ==========================================
// NOTIFICATIONS DIALOG
// ==========================================
@Composable
fun NotificationsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = Color(0xFF4F46E5))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Board Announcements", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotificationItem("📢 PCTB Curriculum 2026", "Tarjuma-tul-Quran is active across 9th-12th. Pakistan Studies added for 10th & 12th.", "1 hour ago")
                NotificationItem("🎯 New MCQs Quiz Engine", "Practice 10,000+ chapter-wise questions with 30s timers and instant results.", "Today")
                NotificationItem("🔥 Study Streak Active", "Keep studying daily to maintain your 5-day streak and earn achievement badges!", "Active")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
            }
        }
    )
}

@Composable
fun NotificationItem(title: String, desc: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(time, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 15.sp)
        }
    }
}

// ==========================================
// PROFILE DIALOG
// ==========================================
@Composable
fun ProfileDialog(
    userName: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
    onFeedback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF4F46E5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(userEmail.ifBlank { "Registered Student" }, fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItem(
                    headlineContent = { Text("Share StudyPoint") },
                    leadingContent = { Icon(Icons.Default.Share, null, tint = Color(0xFF4F46E5)) },
                    modifier = Modifier.clickable { onDismiss(); onShare() }
                )
                ListItem(
                    headlineContent = { Text("WhatsApp Community") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF10B981)) },
                    modifier = Modifier.clickable { onDismiss(); onWhatsApp() }
                )
                ListItem(
                    headlineContent = { Text("Help & Feedback") },
                    leadingContent = { Icon(Icons.Default.Email, null, tint = Color(0xFF3B82F6)) },
                    modifier = Modifier.clickable { onDismiss(); onFeedback() }
                )
                ListItem(
                    headlineContent = { Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFEF4444)) },
                    modifier = Modifier.clickable { onDismiss(); onLogout() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF64748B))
            }
        }
    )
}

// ==========================================
// PRE-EXISTING WIDGETS (MAINTAINED)
// ==========================================
@Composable
fun DrawerHeader(userName: String = "Student", userEmail: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.mipmap.ic_logo_studypoint),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("StudyPoint", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(userName, color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("PCTB Official Curriculum", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun QuotesCarousel() {
    val quotes = remember {
        listOf(
            QuoteItem("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ", "Allahumma Salli 'Ala Muhammadin Wa 'Ala Aali Muhammad"),
            QuoteItem("رَبِّ زِدْنِي عِلْمًا", "My Lord, increase me in knowledge. (Surah Taha 20:114)"),
            QuoteItem("الْعِلْمُ خَيْرٌ مِنَ الْمَالِ", "Knowledge is better than wealth. (Hazrat Ali RA)"),
            QuoteItem("طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ", "Seeking knowledge is an obligation upon every Muslim.")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.layout_quotes_carousel, null, false)
                val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerQuotes)
                val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutQuoteDots)

                viewPager.isUserInputEnabled = true
                val adapter = QuotesCarouselAdapter(quotes)
                viewPager.adapter = adapter

                com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val runnable = object : Runnable {
                    override fun run() {
                        val count = viewPager.adapter?.itemCount ?: 0
                        if (count > 0) {
                            val nextItem = (viewPager.currentItem + 1) % count
                            viewPager.currentItem = nextItem
                        }
                        handler.postDelayed(this, 4000)
                    }
                }
                handler.postDelayed(runnable, 4000)

                viewPager.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                            handler.removeCallbacks(runnable)
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(runnable)
                            handler.postDelayed(runnable, 4000)
                        }
                    }
                    false
                }

                view.tag = Triple(handler, runnable, viewPager)
                view
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}

@Composable
fun AnnouncementCarousel() {
    val announcements = remember { mutableStateOf<List<BoardAnnouncement>>(emptyList()) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("Announcements")
            .whereEqualTo("is_active", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BoardAnnouncement::class.java)
                    }
                    announcements.value = list
                } else {
                    db.collection("board_announcements")
                        .whereEqualTo("is_active", true)
                        .addSnapshotListener { snap2, _ ->
                            if (snap2 != null) {
                                val list2 = snap2.documents.mapNotNull { doc ->
                                    doc.toObject(BoardAnnouncement::class.java)
                                }
                                announcements.value = list2
                            }
                        }
                }
            }
        onDispose {
            listener.remove()
        }
    }

    if (announcements.value.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.layout_announcement_slider, null, false)
                    val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerAnnouncements)
                    val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutDots)

                    val adapter = AnnouncementAdapter(announcements.value)
                    viewPager.adapter = adapter

                    com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

                    viewPager.setPageTransformer { page, position ->
                        val r = 1 - kotlin.math.abs(position)
                        page.scaleY = (0.85f + r * 0.15f)
                        page.alpha = (0.5f + r * 0.5f)
                    }

                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val runnable = object : Runnable {
                        override fun run() {
                            val count = viewPager.adapter?.itemCount ?: 0
                            if (count > 0) {
                                val nextItem = (viewPager.currentItem + 1) % count
                                viewPager.currentItem = nextItem
                            }
                            handler.postDelayed(this, 4000)
                        }
                    }
                    handler.postDelayed(runnable, 4000)

                    view.tag = Triple(handler, runnable, viewPager)
                    view
                },
                update = { view ->
                    val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerAnnouncements)
                    (viewPager.adapter as? AnnouncementAdapter)?.updateData(announcements.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

@Composable
fun ExpandableClassItem(title: String, subjects: List<String>, onClassClick: (String) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "")

    Column {
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.School, null) },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            },
            selected = false,
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 48.dp)) {
                subjects.forEach { subject ->
                    Text(
                        text = subject,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val gradeNum = title.filter { it.isDigit() }.ifBlank { "9" }
                                val intent = Intent(context, MaterialSelectionActivity::class.java).apply {
                                    putExtra("grade", gradeNum)
                                    putExtra("class", gradeNum)
                                    putExtra("subject", subject)
                                }
                                context.startActivity(intent)
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsList(query: String, onClassClick: (String) -> Unit) {
    val context = LocalContext.current
    val classes = listOf("9th Class", "10th Class", "11th Class", "12th Class")
    val subjects = listOf("English", "Urdu", "Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "Islamiyat", "Pak Studies", "Tarjuma-tul-Quran")

    val filteredClasses = classes.filter { it.contains(query, ignoreCase = true) }
    val filteredSubjects = subjects.filter { it.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        filteredClasses.forEach { cls ->
            SearchResultItem(cls, Icons.Default.School) {
                onClassClick(cls.filter { it.isDigit() })
            }
        }
        filteredSubjects.forEach { sub ->
            SearchResultItem(sub, Icons.Default.Book) {
                val intent = Intent(context, MaterialSelectionActivity::class.java).apply {
                    putExtra("grade", "9")
                    putExtra("subject", sub)
                }
                context.startActivity(intent)
            }
        }

        if (filteredClasses.isEmpty() && filteredSubjects.isEmpty()) {
            Text(
                "No results found for \"$query\"",
                modifier = Modifier.padding(vertical = 20.dp),
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SearchResultItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF4F46E5))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0F172A)
    )
}

@Composable
fun RecentReadList(items: List<RecentItem>, onItemClick: (RecentItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .wrapContentHeight()
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Book,
                            null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.title,
                                maxLines = 1,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${item.grade}th • ${item.subject}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassCategoriesGrid(onClassClick: (String) -> Unit) {
    val classes = listOf("9th Class", "10th Class", "11th Class", "12th Class")

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClassItem(classes[0], "ترجمۃ القرآن + اسلامیات", Modifier.weight(1f)) { onClassClick("9") }
            ClassItem(classes[1], "ترجمۃ القرآن + مطالعہ پاکستان", Modifier.weight(1f)) { onClassClick("10") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClassItem(classes[2], "ترجمۃ القرآن + اسلامیات", Modifier.weight(1f)) { onClassClick("11") }
            ClassItem(classes[3], "ترجمۃ القرآن + مطالعہ پاکستان", Modifier.weight(1f)) { onClassClick("12") }
        }
    }
}

@Composable
fun ClassItem(title: String, urduTag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color(0xFFEEF2FF),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = urduTag,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = Color(0xFF4F46E5),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BoardResultsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF4F46E5).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Public, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Punjab Board Results & Gazette", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                Text("BISE Lahore, Gujranwala, Rawalpindi, Multan...", fontSize = 11.sp, color = Color(0xFF64748B))
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

// ==========================================
// YouTube Videos Carousel Composable
// ==========================================

@Composable
fun YouTubeVideosSection(
    videos: List<YouTubeVideo>,
    isLoading: Boolean,
    onVideoClick: (YouTubeVideo) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_youtube),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Channel Lectures",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "View Channel →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFFEF4444)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(videos) { video ->
                    YouTubeVideoCard(video = video, onClick = { onVideoClick(video) })
                }
            }
        }
    }
}

@Composable
fun YouTubeVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Video Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .background(Color(0xFF0F172A))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Translucent Gradient Overlay for depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 100f
                            )
                        )
                )

                // YouTube Play Button in Center
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .background(Color(0xCC000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_youtube),
                        contentDescription = "Play",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Subject / Duration tag in bottom right
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCC000000)
                ) {
                    Text(
                        text = video.durationLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // "PCTB 2026" Badge in bottom left
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCCEF4444)
                ) {
                    Text(
                        text = "PCTB 2026",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Video Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1E293B),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = video.channelTitle,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "▶ Watch Now",
                        fontSize = 11.sp,
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// Social Media Hub Composable
// ==========================================

@Composable
fun SocialMediaHubSection(
    onFacebookClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onYouTubeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title Header
            Text(
                text = "Join Our Learning Community 🌟",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Connect with students & teachers across Punjab for syllabus alerts & study support.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: WhatsApp & YouTube
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialActionChip(
                    modifier = Modifier.weight(1f),
                    title = "WhatsApp",
                    subtitle = "24/7 Support",
                    iconRes = R.drawable.ic_whatsapp,
                    bgColor = Color(0xFFECFDF5),
                    textColor = Color(0xFF065F46),
                    onClick = onWhatsAppClick
                )

                SocialActionChip(
                    modifier = Modifier.weight(1f),
                    title = "YouTube",
                    subtitle = "Free Lectures",
                    iconRes = R.drawable.ic_youtube,
                    bgColor = Color(0xFFFEF2F2),
                    textColor = Color(0xFF991B1B),
                    onClick = onYouTubeClick
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Facebook Official Page
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialActionChip(
                    modifier = Modifier.weight(1f),
                    title = "Facebook",
                    subtitle = "Official Page",
                    iconRes = R.drawable.ic_facebook,
                    bgColor = Color(0xFFEFF6FF),
                    textColor = Color(0xFF1E3A8A),
                    onClick = onFacebookClick
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SocialActionChip(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    iconRes: Int,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ==========================================
// IN-APP UPDATE (Firestore driven)
// Create Firestore doc: app_config/android_update with fields:
//   latest_version_code (number), apk_url (string),
//   force_update (boolean), changelog (string)
// ==========================================
data class AppUpdateInfo(
    val latestVersionCode: Int,
    val apkUrl: String,
    val forceUpdate: Boolean,
    val changelog: String
)

fun checkForAppUpdate(context: Context, onResult: (AppUpdateInfo?) -> Unit) {
    try {
        @Suppress("DEPRECATION")
        val currentCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        FirebaseFirestore.getInstance()
            .collection("app_config")
            .document("android_update")
            .get()
            .addOnSuccessListener { doc ->
                val latest = doc.getLong("latest_version_code")?.toInt() ?: 0
                if (latest > currentCode) {
                    onResult(
                        AppUpdateInfo(
                            latestVersionCode = latest,
                            apkUrl = doc.getString("apk_url").orEmpty(),
                            forceUpdate = doc.getBoolean("force_update") ?: false,
                            changelog = doc.getString("changelog").orEmpty()
                        )
                    )
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    } catch (e: Exception) {
        onResult(null)
    }
}

@Composable
fun AppUpdateDialog(info: AppUpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!info.forceUpdate) onDismiss() },
        title = { Text("Update Available \uD83C\uDF89", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("A new version of StudyPoint is available. Update now for the latest features & fixes.")
                if (info.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(info.changelog, fontSize = 13.sp, color = Color(0xFF64748B))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (info.apkUrl.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
                    }
                    if (!info.forceUpdate) onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!info.forceUpdate) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    )
}
