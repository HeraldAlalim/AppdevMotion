package com.example.seglo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.example.seglo.R
import com.example.seglo.ui.components.CustomTextField
import com.example.seglo.ui.components.LoginButton
import com.example.seglo.ui.components.SignUpText
import com.example.seglo.ui.theme.AccentBlue
import com.example.seglo.ui.theme.DarkGray
import com.example.seglo.ui.theme.SeGloTheme

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SeGloTheme {
        LoginScreen()
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    errorMessage: String? = null

) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val isFormValid = username.isNotBlank() && password.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD5D4F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Image (about 40% of the screen)
            Image(
                painter = painterResource(id = R.drawable.login),
                contentDescription = "Login Character",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                contentScale = ContentScale.Crop
            )

            // Login Card (about 50% of the screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back!",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Login",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    CustomTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        isPassword = true
                    )
                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, bottom = 8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = AccentBlue,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))

                    // Show error message if present
                    val errorToShow = localErrorMessage ?: errorMessage
                    if (!errorToShow.isNullOrEmpty()) {
                        Text(
                            text = errorToShow,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    LoginButton(
                        onClick = {
                            if (isFormValid) {
                                onLoginClick(username, password)
                            }
                        },
                        enabled = isFormValid
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SignUpText(onSignUpClick = onSignUpClick)
                }
            }
        }
    }
}