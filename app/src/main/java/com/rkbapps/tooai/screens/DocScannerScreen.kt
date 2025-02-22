package com.rkbapps.tooai.screens

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.constants.ScanModes
import com.rkbapps.tooai.utils.copyFileToExternalStorage
import com.rkbapps.tooai.utils.getActivity
import java.io.File

class DocScannerScreen : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val resultInfo = rememberSaveable {
            mutableStateOf("")
        }

        val result = rememberSaveable {
            mutableStateOf<GmsDocumentScanningResult?>(null)
        }

        val scannerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                handelScanActivityResult(activityResult, result, context, navigator!!)
            }
        LaunchedEffect(key1 = Unit) {
            try {
                onScanButtonClick(
                    activity = context.getActivity() as Activity,
                    scannerLauncher = scannerLauncher,
                    resultInfo = resultInfo
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Something went wrong.", Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = {
                TopBar(title = "Doc Scanner") {
                    navigator!!.pop()
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
            ) {
                if (result.value != null) {
                    Text(
                        text = "Final Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    val pages = result.value!!.pages ?: emptyList()
                    if (pages.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(count = pages.size) { position ->
                                val uri = pages[position].imageUri
                                uri.path.let { path ->
                                    if (path != null) {
                                        val externalUri = getExternalUri(context, path)
                                        if (externalUri != null) {
                                            AsyncImage(
                                                model = externalUri,
                                                contentDescription = "",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                    val pdf = result.value!!.pdf
                    if (pdf != null) {
                        val uri = pdf.uri
                        uri.path.let { path ->
                            if (path != null) {
                                val externalUri = getExternalUri(context, path)
                                if (externalUri != null) {
                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        onClick = {
                                            if (copyFileToExternalStorage(
                                                    context,
                                                    externalUri,
                                                    "TooAi"
                                                )
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    "Saved in \"Documents/TooAi\" folder.",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Unable to save. Something went wrong.",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            }
                                        }) {
                                        Text(text = "Save PDF")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onScanButtonClick(
        activity: Activity,
        scanMode: MutableIntState = mutableIntStateOf(ScanModes.FULL_MODE),
        scannerLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        resultInfo: MutableState<String>
    ) {
        val options =
            GmsDocumentScannerOptions.Builder()
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                )
                .setGalleryImportAllowed(true)
                .setScannerMode(scanMode.intValue)

        GmsDocumentScanning.getClient(options.build())
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender: IntentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener() { e: Exception ->
                resultInfo.value = e.message.toString()
            }
    }

    private fun handelScanActivityResult(
        activityResult: ActivityResult,
        resultInfo: MutableState<GmsDocumentScanningResult?>,
        context: Context,
        navigator: Navigator
    ) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (resultCode == Activity.RESULT_OK && result != null) {
            resultInfo.value = result
        } else if (resultCode == Activity.RESULT_CANCELED) {
            navigator.pop()
            Toast.makeText(context, "Canceled by user.", Toast.LENGTH_SHORT).show()
//            resultInfo.value = "Canceled by user."
        } else {
            navigator.pop()
            Toast.makeText(context, "Failed to scan.", Toast.LENGTH_SHORT).show()
//            resultInfo.value = "Failed to scan."
        }
    }

    private fun getExternalUri(context: Context, path: String): Uri? {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            File(path)
        )
    }
}