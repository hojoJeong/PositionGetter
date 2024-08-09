package com.galaxy.positiongetter

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.galaxy.positiongetter.ui.theme.PositionGetterTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi


class MainActivity : ComponentActivity() {

    // todo : Set package name of installed app on your device
    val pkgList = listOf(
        PackageDto("Gallery", "pkg name"),
        PackageDto("Message", "pkg name"),
        PackageDto("Calendar", "pkg name"),
        PackageDto("Note", "pkg name"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PositionGetterTheme {
                CheckPermission()
            }
        }
    }

    @Composable
    private fun CheckPermission() {

        Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        // Request Accessibility Permission
        if(!checkServiceRunning(PositionGetterService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Overlay Permission State
        val permissionState = rememberSaveable { mutableStateOf(false) }
        val overlayPermissionState = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()) {
            if(Settings.canDrawOverlays(this)) {
                permissionState.value = true
            }
            else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
                permissionState.value = false
                finish()
            }
        }

        if(Settings.canDrawOverlays(this)) {
            permissionState.value = true
        }
        else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            SideEffect { overlayPermissionState.launch(intent) }
            permissionState.value = false
        }

        if (permissionState.value) InitView()
    }

    @Composable
    private fun InitView() {

        val pkgList = rememberSaveable { mutableStateOf(pkgList) }
        val selectedItem = rememberSaveable { mutableStateOf(PackageDto("", "")) }
        val startServiceBtnActivation = rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(0.dp, 0.dp, 0.dp, 80.dp)
                .fillMaxSize()
                .background(color = colorResource(id = R.color.white)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {

            AppListColumn(pkgList, selectedItem, startServiceBtnActivation)
            SelectedAppTitleText(selectedItem)
            StartServiceButton(selectedItem, startServiceBtnActivation.value)
        }
    }

    @Composable
    private fun AppListColumn(
        pkgList: MutableState<List<PackageDto>>,
        selectedItem: MutableState<PackageDto>,
        startServiceBtnActivation: MutableState<Boolean>
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(40.dp)
        ) {
            itemsIndexed(
                items = pkgList.value
            ) { _, item ->
                Button(
                    modifier = Modifier.padding(0.dp, 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, color = colorResource(id = R.color.black_alpha50)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.black_alpha10),
                        contentColor = colorResource(id = R.color.black)
                    ),
                    onClick = {
                        if (checkAppInstalled(item.pkgName)) {
                            selectedItem.value = item
                            startServiceBtnActivation.value = true
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Not Installed App",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedItem.value = PackageDto("Not Installed", "")
                            startServiceBtnActivation.value = false
                        }
                    }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = item.appName, fontWeight = FontWeight.Bold
                        )
                        Text(text = item.pkgName)
                    }
                }
            }
        }
    }

    @Composable
    private fun SelectedAppTitleText(selectedItem: MutableState<PackageDto>) {
        Text(
            modifier = Modifier.padding(10.dp),
            text = "Selected App : ${selectedItem.value.appName}"
        )
    }

    @Composable
    private fun StartServiceButton(selectedItem: MutableState<PackageDto>, activation: Boolean) {
        Button(
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(2.dp, color = colorResource(id = R.color.black)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.white),
                contentColor = colorResource(id = R.color.black)
            ),
            enabled = activation,
            onClick = { startService(selectedItem.value.pkgName) }) {
            Text(text = "Get Position")
        }
    }

    private fun startService(pkgName: String) {
        Intent(this@MainActivity, PositionGetterService::class.java).apply {
            putExtra("Get Position", true)
            putExtra("pkg", pkgName)
            startService(this)
        }
    }

    private fun checkAppInstalled(packageName: String): Boolean {

        return try {

            val packageManager = this.packageManager
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun checkServiceRunning(service: Class<*>) : Boolean {
        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == service.name }
    }

    @Preview(showBackground = true)
    @Composable
    fun InitViewPreview() {
        PositionGetterTheme {
            InitView()
        }
    }
}


