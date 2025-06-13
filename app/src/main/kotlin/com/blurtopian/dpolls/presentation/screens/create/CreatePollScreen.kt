package com.blurtopian.dpolls.presentation.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blurtopian.dpolls.presentation.theme.PollsDAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    onNavigateBack: () -> Unit,
    onPollCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mutableListOf("", "")) }
    var duration by remember { mutableStateOf("24") }
    var isCreating by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Create Poll",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poll Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Poll Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            // Poll Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
            
            // Options Section
            item {
                Text(
                    text = "Poll Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            itemsIndexed(options) { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { newValue ->
                            options[index] = newValue
                        },
                        label = { Text("Option ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    if (options.size > 2) {
                        IconButton(
                            onClick = {
                                options.removeAt(index)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove option",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Add Option Button
            item {
                OutlinedButton(
                    onClick = {
                        if (options.size < 10) {
                            options.add("")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = options.size < 10
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Option")
                }
            }
            
            // Duration
            item {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (hours)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            
            // Create Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        isCreating = true
                        // TODO: Implement poll creation logic
                        // For now, just simulate creation
                        onPollCreated()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating && title.isNotBlank() && 
                             description.isNotBlank() && 
                             options.all { it.isNotBlank() } &&
                             duration.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating...")
                    } else {
                        Text("Create Poll")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreatePollScreenPreview() {
    PollsDAppTheme {
        CreatePollScreen(
            onNavigateBack = {},
            onPollCreated = {}
        )
    }
}

