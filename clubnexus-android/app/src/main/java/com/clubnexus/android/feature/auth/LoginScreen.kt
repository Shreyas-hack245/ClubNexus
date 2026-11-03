package com.clubnexus.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: SessionViewModel) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Core") }
    var position by remember { mutableStateOf("Member") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ClubNexus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text("Club Management System", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        if (register) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (register) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(department, { department = it }, Modifier.fillMaxWidth(), label = { Text("Department") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(position, { position = it }, Modifier.fillMaxWidth(), label = { Text("Position") }, singleLine = true)
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Sign in with your ClubNexus account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        viewModel.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (register) viewModel.register(name, email, password, department, position)
                else viewModel.login(email, password)
            },
            modifier = Modifier.fillMaxWidth(), enabled = !viewModel.busy && email.isNotBlank() && password.length >= 6 && (!register || name.length >= 2)
        ) {
            if (viewModel.busy) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            else Text(if (register) "Create account" else "Sign in")
        }
        TextButton(onClick = { register = !register; viewModel.clearError() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (register) "Use an existing account" else "Create a new member account")
        }
    }
}
