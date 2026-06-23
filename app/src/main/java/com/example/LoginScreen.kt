package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Dynamic Colors based on mode
    val obsidianDark = if (isDarkMode) Color(0xFF060913) else Color(0xFFF1F5F9)
    val panelBg = if (isDarkMode) Color(0xFF0D1322) else Color(0xFFFFFFFF)
    val emeraldAccent = if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669)
    val textMain = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSub = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val dangerRose = if (isDarkMode) Color(0xFFF43F5E) else Color(0xFFDC2626)
    val borderTone = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val gridBorderTone = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(obsidianDark),
        contentAlignment = Alignment.Center
    ) {
        // Theme switcher at top right
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
                .testTag("theme_toggle_button")
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                contentDescription = "Toggle Theme",
                tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF475569)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = panelBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .border(1.dp, borderTone, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Application Logo loaded dynamically
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://i.postimg.cc/3x9HWhJ2/Whats-App-Image-2026-06-02-at-10-33-41.jpg")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, emeraldAccent, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Login RzKredit",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = textMain,
                        letterSpacing = (-0.5).sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Akses RzKredit Mobile - Administrasi Kredit Premium",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textSub,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = ""
                    },
                    label = { Text("Username") },
                    placeholder = { Text("Ketik rzkarim") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = textSub)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = gridBorderTone,
                        focusedLabelColor = emeraldAccent,
                        unfocusedLabelColor = textSub
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("Sandi") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = textSub)
                    },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "SEMBUNYI" else "LIHAT",
                                color = emeraldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = gridBorderTone,
                        focusedLabelColor = emeraldAccent,
                        unfocusedLabelColor = textSub
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = dangerRose,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.testTag("login_error_message")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (username == "rzkarim" && password == "rzkarim123") {
                            onLoginSuccess()
                        } else {
                            errorMessage = "❌ Username atau Sandi Salah!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_button")
                ) {
                    Text(
                        text = "MASUK DASHBOARD",
                        fontWeight = FontWeight.Black,
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
