package dev.thoq.gleamstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.thoq.gleamstorm.utils.misc.ansiToAnnotatedString
import dev.thoq.gleamstorm.utils.state.ProjectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun Terminal(projectState: ProjectState) {
    var command by remember { mutableStateOf(TextFieldValue("")) }
    val output = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val runCommand = {
        val commandText = command.text
        if(commandText.isNotBlank()) {
            output.add("> $commandText")
            command = TextFieldValue("")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val process = ProcessBuilder(commandText.split(" "))
                        .directory(projectState.projectFolder)
                        .redirectErrorStream(true)
                        .start()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while(reader.readLine().also { line = it } != null) {
                        output.add(line!!)
                    }
                    process.waitFor()
                } catch(e: Exception) {
                    output.add(e.message ?: "An error occurred")
                }
            }
        }
    }

    LaunchedEffect(output.size) {
        if(output.isNotEmpty()) {
            listState.animateScrollToItem(index = output.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(output) { line ->
                Text(text = ansiToAnnotatedString(line))
            }
        }
        TextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(color = Color.White),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { runCommand() })
        )
    }
}
