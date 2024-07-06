package com.autogdpr.http3urlgetter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autogdpr.http3urlgetter.ui.theme.HTTP3URLGetterTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HTTP3URLGetterTheme {
                // A surface container using the 'background' color from the theme
                val httpClient = HTTPClient(applicationContext)
                App(httpClient)
            }
        }
    }

    @Composable
    fun ResponseBody(responseBody: String) {
        Box(modifier = Modifier.fillMaxSize()) {
            TextField(
                value = responseBody,
                onValueChange = {},
                modifier = Modifier.fillMaxSize()
                    .padding(8.dp),
                readOnly = true,
                singleLine = false
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun App(httpClient: HTTPClient) {

        var urlAddress by rememberSaveable { mutableStateOf("") }
        var urlBody by rememberSaveable { mutableStateOf("") }
        var urlProtocol by rememberSaveable { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SelectionContainer {
                Row {
                    Column (
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    ){
                        DisableSelection {
                            Text(text = "Welcome. Insert URL below", textAlign = TextAlign.Left)
                        }
                        OutlinedTextField(
                            value = urlAddress,
                            onValueChange = { urlAddress = it },
                            label = { Text("URL") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                // Aquí ejecutas la acción deseada al hacer clic en "Done" en el teclado
                                // Por ejemplo, podrías ejecutar la solicitud GET aquí
                                val (responseProtocol, responseBody) = httpClient.performGetRequest(urlAddress)
                                urlProtocol = responseProtocol
                                urlBody = responseBody
                                keyboardController?.hide()
                            })
                        )
                        OutlinedButton(onClick = {
                            val (protocol, responseBody) = httpClient.performGetRequest(urlAddress)
                            urlProtocol = protocol
                            urlBody = responseBody
                            keyboardController?.hide()
                        }
                        ){
                            Text("GET request")
                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        Text(text = urlProtocol)
                        ResponseBody(responseBody = urlBody) // Pasar el cuerpo de la respuesta a la función ResponseBody
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Preview() {

        val httpClient = HTTPClient(applicationContext)
        App(httpClient)

    }

}