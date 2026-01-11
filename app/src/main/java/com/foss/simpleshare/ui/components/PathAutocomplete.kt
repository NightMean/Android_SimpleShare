package com.foss.simpleshare.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction 
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PathAutocomplete(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onPathChange: (String) -> Unit, 
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf(emptyList<File>()) }
    var isFocused by remember { mutableStateOf(false) }

    // Logic to list files
    LaunchedEffect(value.text, isFocused) {
        if (!isFocused) {
            expanded = false
            return@LaunchedEffect
        }

        // Run on IO
        val newSuggestions = withContext(Dispatchers.IO) {
            val input = value.text
            val file = File(input)
            
            // Determine search directory and filter prefix
            val searchDir: File?
            val prefix: String
            
            if (input.endsWith("/")) {
                searchDir = file
                prefix = ""
            } else {
                searchDir = file.parentFile
                prefix = file.name
            }

            if (searchDir != null && searchDir.exists() && searchDir.isDirectory) {
                // List directories only
                searchDir.listFiles()?.filter { 
                    it.isDirectory && it.name.startsWith(prefix, ignoreCase = true) 
                }?.sortedBy { it.name } ?: emptyList()
            } else {
                emptyList()
            }
        }

        suggestions = newSuggestions
        expanded = newSuggestions.isNotEmpty()
    }

    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                onPathChange(newValue.text)
                // Expansion is handled by LaunchedEffect
            },
            label = label,
            isError = isError,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                }
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size
                },
            singleLine = true,
             colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            )
        )

        // Custom Popup to strictly anchor BELOW the TextField
        if (expanded && isFocused) {
             androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, textFieldSize.height),
                properties = PopupProperties(focusable = false, clippingEnabled = false),
                onDismissRequest = { expanded = false }
            ) {
                 // Match width of TextField
                 androidx.compose.material3.Surface(
                     modifier = Modifier
                         .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                         .heightIn(max = 200.dp),
                     shape = RoundedCornerShape(4.dp),
                     shadowElevation = 4.dp,
                     color = MaterialTheme.colorScheme.surface,
                     tonalElevation = 3.dp // Corrected param name
                 ) {
                     LazyColumn {
                         items(suggestions) { file ->
                             DropdownMenuItem(
                                 text = { Text(file.name) },
                                 onClick = {
                                    val currentText = value.text
                                    // Replace the last segment with the full folder name + slash
                                    val newPath = if (currentText.endsWith("/")) {
                                        currentText + file.name + "/"
                                    } else {
                                        val parent = currentText.substringBeforeLast('/', "")
                                        if (parent.isEmpty() && !currentText.startsWith("/")) {
                                            file.absolutePath + "/" 
                                        } else {
                                            val prefix = currentText.substring(0, currentText.lastIndexOf('/') + 1)
                                            prefix + file.name + "/"
                                        }
                                    }
                                    
                                    onValueChange(
                                        TextFieldValue(
                                            text = newPath,
                                            selection = TextRange(newPath.length) // Move cursor to end
                                        )
                                    )
                                    onPathChange(newPath)
                                    expanded = false
                                 }
                             )
                         }
                     }
                 }
            }
        }
    }
}
