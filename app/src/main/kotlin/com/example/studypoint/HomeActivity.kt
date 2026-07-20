package com.example.studypoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.launch

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

                    userName = auth.currentUser?.displayName
                        ?: auth.currentUser?.email
                        ?: "Student",

                    onStartStudying = {
                        startActivity(
                            Intent(
                                this,
                                GradeSelectionActivity::class.java
                            )
                        )
                    },

                    onLogout = { logout() },

                    onFeedback = { sendFeedback() },

                    onWhatsApp = { openWhatsApp() },

                    onShare = { shareApp() }

                )
            }
        }
    }

    private fun logout() {

        auth.signOut()

        Toast.makeText(
            this,
            "Logged out successfully",
            Toast.LENGTH_SHORT
        ).show()

        startActivity(
            Intent(
                this,
                LoginActivity::class.java
            )
        )

        finish()
    }

    private fun sendFeedback() {

        val intent = Intent(Intent.ACTION_SENDTO)

        intent.data = Uri.parse("mailto:")

        intent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf("alsamadacadmey6844@gmail.com")
        )

        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            "StudyPoint Feedback"
        )

        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Hello StudyPoint Team,\n\nI want to share my feedback.\n\nRegards,\n${auth.currentUser?.displayName}"
        )

        startActivity(intent)
    }

    private fun openWhatsApp() {

        startActivity(

            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://whatsapp.com/channel/0029Vah4pV3Au3aVHGzJL805")
            )

        )

    }

    private fun shareApp() {

        val intent = Intent(Intent.ACTION_SEND)

        intent.type = "text/plain"

        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Download StudyPoint App.\nNotes • MCQs • Past Papers"
        )

        startActivity(
            Intent.createChooser(intent, "Share")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(

    userName: String,

    onStartStudying: () -> Unit,

    onLogout: () -> Unit,

    onFeedback: () -> Unit,

    onWhatsApp: () -> Unit,

    onShare: () -> Unit

) {

    val drawerState =
        rememberDrawerState(DrawerValue.Closed)

    val scope =
        rememberCoroutineScope()

    var selectedIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet {

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "📚 StudyPoint",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.headlineSmall
                )

                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = selectedIndex == 0,
                    onClick = {
                        selectedIndex = 0
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Notes") },
                    selected = false,
                    onClick = {}
                )

                NavigationDrawerItem(
                    label = { Text("MCQs") },
                    selected = false,
                    onClick = {}
                )

                NavigationDrawerItem(
                    label = { Text("Past Papers") },
                    selected = false,
                    onClick = {}
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {}
                )

                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = onLogout
                )

            }

        }

    ) {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {

                        Text("StudyPoint")

                    },

                    navigationIcon = {

                        IconButton(
                            onClick = {

                                scope.launch {
                                    drawerState.open()
                                }

                            }
                        ) {

                            Icon(
                                Icons.Default.Menu,
                                null
                            )

                        }

                    },

                    actions = {

                        IconButton(onClick = {}) {

                            Icon(
                                Icons.Default.Search,
                                null
                            )

                        }

                        IconButton(onClick = {}) {

                            Icon(
                                Icons.Default.Notifications,
                                null
                            )

                        }

                        IconButton(onClick = {}) {

                            Icon(
                                Icons.Default.AccountCircle,
                                null
                            )

                        }

                    }

                )

            }

        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF1E3A8A))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Welcome 👋",
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Text(
                            text = userName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        var searchText by remember {
                            mutableStateOf("")
                        }

                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Search Notes, MCQs, Papers...")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp)
                        )

                    }

                }

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(R.mipmap.ic_logo_studypoint),
                    contentDescription = "Logo",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "StudyPoint",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Learn Smart • Score High",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(25.dp))

                Button(
                    onClick = onStartStudying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Icon(Icons.Default.PlayArrow, null)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Start Studying")

                }

                Spacer(modifier = Modifier.height(25.dp))

                Text(
                    text = "Quick Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    HomeActionCard(
                        icon = Icons.Default.Book,
                        title = "Notes",
                        onClick = { }
                    )

                    HomeActionCard(
                        icon = Icons.Default.Home,
                        title = "Classes",
                        onClick = { }
                    )

                    HomeActionCard(
                        icon = Icons.Default.Notifications,
                        title = "Alerts",
                        onClick = { }
                    )

                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    HomeActionCard(
                        icon = Icons.Default.Person,
                        title = "Feedback",
                        onClick = onFeedback
                    )

                    HomeActionCard(
                        icon = Icons.Default.Share,
                        title = "WhatsApp",
                        onClick = onWhatsApp
                    )

                    HomeActionCard(
                        icon = Icons.Default.Share,
                        title = "Share",
                        onClick = onShare
                    )

                }

                Spacer(modifier = Modifier.weight(1f))

                NavigationBar {

                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = {
                            Icon(Icons.Default.Home, null)
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Icon(Icons.Default.Book, null)
                        },
                        label = {
                            Text("Notes")
                        }
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Icon(Icons.Default.Search, null)
                        },
                        label = {
                            Text("Search")
                        }
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Icon(Icons.Default.Notifications, null)
                        },
                        label = {
                            Text("Alerts")
                        }
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Icon(Icons.Default.AccountCircle, null)
                        },
                        label = {
                            Text("Profile")
                        }
                    )

                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "© StudyPoint 2026",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

            } // Column

        } // Scaffold

    } // Drawer

} // HomeScreen
@Composable
fun HomeActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(100.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title)
        }
    }
}