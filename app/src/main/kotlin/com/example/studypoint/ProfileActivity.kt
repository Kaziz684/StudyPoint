package com.example.studypoint

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF4F46E5),
                    onSurface = Color(0xFF0F172A),
                    surface = Color.White,
                    onSurfaceVariant = Color(0xFF475569)
                )
            ) {
                ProfileScreen(
                    onBackClick = { finish() },
                    onProfileUpdated = {
                        setResult(RESULT_OK)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 1. Load saved profile data
    val initialProfile = remember { ProfileManager.getProfile(context) }
    var isEditMode by remember { mutableStateOf(true) }

    var fullName by remember { mutableStateOf(initialProfile.fullName) }
    var age by remember { mutableStateOf(initialProfile.age) }
    var schoolCollege by remember { mutableStateOf(initialProfile.schoolCollege) }
    var selectedGrade by remember { mutableStateOf(initialProfile.grade) }
    var selectedSubjectGroup by remember { mutableStateOf(initialProfile.subjectGroup) }
    var imagePath by remember { mutableStateOf(initialProfile.imagePath) }

    // Error states for validation
    var nameError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var schoolError by remember { mutableStateOf<String?>(null) }

    // Dialog for photo source selection (Camera vs Gallery)
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    // Activity result launcher for Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ProfileManager.saveProfileImageFromUri(context, uri)
            if (savedPath.isNotBlank()) {
                imagePath = savedPath
                Toast.makeText(context, "Profile picture updated! 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity result launcher for Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = ProfileManager.saveProfileImageBitmap(context, bitmap)
            if (savedPath.isNotBlank()) {
                imagePath = savedPath
                Toast.makeText(context, "Profile photo captured! 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Punjab Curriculum (PCTB) Student", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                // Validate and Save
                                var isValid = true
                                if (fullName.trim().isEmpty()) {
                                    nameError = "Name cannot be empty"
                                    isValid = false
                                } else {
                                    nameError = null
                                }

                                val ageNum = age.trim().toIntOrNull()
                                if (ageNum == null || ageNum < 5 || ageNum > 80) {
                                    ageError = "Enter valid age (5 - 80)"
                                    isValid = false
                                } else {
                                    ageError = null
                                }

                                if (schoolCollege.trim().isEmpty()) {
                                    schoolError = "School/College name cannot be empty"
                                    isValid = false
                                } else {
                                    schoolError = null
                                }

                                if (isValid) {
                                    val updated = UserProfile(
                                        fullName = fullName.trim(),
                                        age = age.trim(),
                                        schoolCollege = schoolCollege.trim(),
                                        grade = selectedGrade,
                                        subjectGroup = selectedSubjectGroup,
                                        imagePath = imagePath
                                    )
                                    ProfileManager.saveProfile(context, updated)
                                    isEditMode = true
                                    onProfileUpdated()
                                    Toast.makeText(context, "Profile details saved successfully! ✅", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isEditMode = true
                            }
                        }
                    ) {
                        Text(
                            text = if (isEditMode) "Save" else "Edit",
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ==========================================
            // Circular Avatar with Camera overlay
            // ==========================================
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarFile = remember(imagePath) {
                        if (imagePath.isNotBlank()) File(imagePath) else null
                    }

                    if (avatarFile != null && avatarFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        // Initials fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fullName.trim().take(1).uppercase().ifBlank { "S" },
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                // Edit Camera Button badge
                IconButton(
                    onClick = { showPhotoPickerSheet = true },
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5))
                        .border(2.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fullName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = buildString {
                    append(selectedGrade)
                    if (selectedSubjectGroup.isNotBlank()) {
                        append(" • $selectedSubjectGroup")
                    }
                },
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // Profile Information Card
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {

                    Text(
                        text = "Personal Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (it.isNotBlank()) nameError = null
                        },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF4F46E5)) },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        enabled = isEditMode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Age
                    OutlinedTextField(
                        value = age,
                        onValueChange = {
                            age = it
                            if (it.isNotBlank()) ageError = null
                        },
                        label = { Text("Age (Years)") },
                        leadingIcon = { Icon(Icons.Default.Cake, null, tint = Color(0xFF4F46E5)) },
                        isError = ageError != null,
                        supportingText = ageError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        enabled = isEditMode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. School / College Name
                    OutlinedTextField(
                        value = schoolCollege,
                        onValueChange = {
                            schoolCollege = it
                            if (it.isNotBlank()) schoolError = null
                        },
                        label = { Text("School / College Name") },
                        leadingIcon = { Icon(Icons.Default.School, null, tint = Color(0xFF4F46E5)) },
                        isError = schoolError != null,
                        supportingText = schoolError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        enabled = isEditMode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Class / Grade Selection
                    Text(
                        text = "Enrolled Class / Grade (PCTB)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val gradeList = listOf("Class 9th", "Class 10th", "Class 11th", "Class 12th")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gradeList.forEach { gr ->
                            val isSelected = gr == selectedGrade
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = isEditMode) {
                                        selectedGrade = gr
                                        selectedSubjectGroup = ""
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gr.replace("Class ", ""),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Subject / Group Selection (depends on class)
                    val isMatric = selectedGrade == "Class 9th" || selectedGrade == "Class 10th"
                    val subjectOptions = if (isMatric) {
                        listOf("Biology", "Computer Science")
                    } else {
                        listOf("Pre-Medical", "Pre-Engineering", "ICS", "Commerce", "Arts")
                    }
                    Text(
                        text = if (isMatric) "Subject (9th/10th)" else "Group (11th/12th)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjectOptions.chunked(2).forEach { rowOpts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOpts.forEach { opt ->
                                    val isSelected = opt == selectedSubjectGroup
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(enabled = isEditMode) { selectedSubjectGroup = opt },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) Color(0xFF10B981) else Color(0xFFF1F5F9),
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = opt,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                // Fill empty space if odd number of items
                                if (rowOpts.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = {
                    if (isEditMode) {
                        // Validate and Save
                        var isValid = true
                        if (fullName.trim().isEmpty()) {
                            nameError = "Name cannot be empty"
                            isValid = false
                        }
                        val ageNum = age.trim().toIntOrNull()
                        if (ageNum == null || ageNum < 5 || ageNum > 80) {
                            ageError = "Enter valid age (5 - 80)"
                            isValid = false
                        }
                        if (schoolCollege.trim().isEmpty()) {
                            schoolError = "School/College name cannot be empty"
                            isValid = false
                        }

                        if (isValid) {
                            val updated = UserProfile(
                                fullName = fullName.trim(),
                                age = age.trim(),
                                schoolCollege = schoolCollege.trim(),
                                grade = selectedGrade,
                                subjectGroup = selectedSubjectGroup,
                                imagePath = imagePath
                            )
                            ProfileManager.saveProfile(context, updated)
                            isEditMode = true
                            onProfileUpdated()
                            Toast.makeText(context, "Profile details saved successfully! ✅", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        isEditMode = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditMode) Color(0xFF10B981) else Color(0xFF4F46E5)
                )
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Save Changes" else "Edit Profile Details",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // ==========================================
    // Photo Source Selector Modal Dialog
    // ==========================================
    if (showPhotoPickerSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerSheet = false },
            title = { Text("Choose Profile Photo", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Camera
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoPickerSheet = false
                                val hasCamPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCamPermission) {
                                    cameraLauncher.launch()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF4F46E5))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Take Photo from Camera", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    // Option 2: Gallery
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoPickerSheet = false
                                galleryLauncher.launch("image/*")
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoPickerSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
