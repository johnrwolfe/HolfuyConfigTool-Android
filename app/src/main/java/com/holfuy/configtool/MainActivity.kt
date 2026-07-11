package com.holfuy.configtool

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holfuy.configtool.device.DeviceRepository
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.device.RealHolfuyDevice
import com.holfuy.configtool.ui.screens.HelpScreen
import com.holfuy.configtool.ui.screens.MainScreen
import com.holfuy.configtool.ui.theme.HolfuyConfigToolTheme
import com.holfuy.configtool.ui.viewmodel.MainViewModel
import com.holfuy.configtool.ui.viewmodel.MainViewModelFactory
import com.holfuy.configtool.usb.AndroidUsbDeviceProvider
import com.holfuy.configtool.usb.HolfuyUsb
import com.holfuy.configtool.usb.UsbDeviceProvider


class MainActivity : ComponentActivity()
{
    companion object
    {
        private const val TAG = "HolfuyUSB-A"
    
        private const val ACTION_USB_PERMISSION =
            "com.holfuy.configtool.USB_PERMISSION"
    }
    
    private lateinit var permissionIntent: PendingIntent 
    private lateinit var mainViewModel: MainViewModel
    private lateinit var usbManager: UsbManager
    private lateinit var holfuyDevice: HolfuyDevice
    private lateinit var usbDeviceProvider: UsbDeviceProvider
    
    private fun getDisplayName(
        contentResolver: ContentResolver,
        uri: Uri
    ): String
    {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
    
            val nameIndex =
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
    
        return "firmware.bin"
    }
    
    private fun registerReceivers()
    {
        registerReceiver(
            usbPermissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            RECEIVER_NOT_EXPORTED
        )
        
        registerReceiver(
            usbAttachReceiver,
            IntentFilter(
                UsbManager.ACTION_USB_DEVICE_ATTACHED
            ),
            RECEIVER_NOT_EXPORTED
        )
        
        registerReceiver(
            usbDetachReceiver,
            IntentFilter(
                UsbManager.ACTION_USB_DEVICE_DETACHED
            ),
            RECEIVER_NOT_EXPORTED
        )    
    }
    
    private fun unregisterReceivers()
    {
        unregisterReceiver(
            usbPermissionReceiver
        )
        
        unregisterReceiver(
            usbAttachReceiver
        )
        
        unregisterReceiver(
            usbDetachReceiver
        )
    }
    
    private fun ensureUsbPermission(
        usbDevice: UsbDevice
    )
    {
        if (usbManager.hasPermission(usbDevice)) {
    
            Log.i(
                TAG,
                "USB permission already granted"
            )
    
            DeviceRepository.setPermissionGranted(true)
            return
        }
    
        Log.i(
            TAG,
            "Requesting USB permission"
        )
    
        usbManager.requestPermission(
            usbDevice,
            permissionIntent
        )
    }
    
    private fun Intent.getSupportedUsbDevice(): UsbDevice?
    {
        val usbDevice =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(
                    UsbManager.EXTRA_DEVICE,
                    UsbDevice::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(
                    UsbManager.EXTRA_DEVICE
                )
            } ?: return null
    
        if (!HolfuyUsb.isSupported(usbDevice)) {
    
            Log.i(
                TAG,
                "Ignoring unsupported USB device productId=0x${usbDevice.productId.toString(16)}"
            )
    
            return null
        }
    
        return usbDevice
    }
    
    private val usbPermissionReceiver =
        object : BroadcastReceiver()
        {
            override fun onReceive(
                context: Context,
                intent: Intent
            )
            {
                Log.d(
                    TAG,
                    "intent extras=${intent.extras}"
                )
                Log.i(
                    TAG,
                    "usbPermissionReceiver action=${intent.action}"
                )
    
                if (intent.action != ACTION_USB_PERMISSION) {
                    return
                }
    
                val granted =
                    intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED,
                        false
                    )
    
                Log.i(
                    TAG,
                    "USB permission response received granted=$granted"
                )
    
                DeviceRepository.setPermissionGranted(
                    granted
                )
            }
        }
        
    private val usbAttachReceiver =
        object : BroadcastReceiver()
        {
            override fun onReceive(
                context: Context,
                intent: Intent
            )
            {
                if (
                    intent.action !=
                    UsbManager.ACTION_USB_DEVICE_ATTACHED
                ) {
                    return
                }
    
                val usbDevice =
                    intent.getSupportedUsbDevice()
                        ?: return
    
                Log.i(
                    TAG,
                    "Supported USB device attached"
                )
    
                DeviceRepository.setAttached(
                    true
                )
     
                ensureUsbPermission( usbDevice )
            }
        }
    
    private val usbDetachReceiver =
        object : BroadcastReceiver()
        {
            override fun onReceive(
                context: Context,
                intent: Intent
            )
            {
                if (
                    intent.action !=
                    UsbManager.ACTION_USB_DEVICE_DETACHED
                ) {
                    return
                }
    
                intent.getSupportedUsbDevice()
                    ?: return
    
                Log.i(
                    TAG,
                    "Supported USB device detached"
                )
                
                holfuyDevice.onUsbDetached()
    
                DeviceRepository.clearConnectionState()

            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
    
        Log.d(
            TAG,
            "onCreate savedInstanceState=${savedInstanceState != null}"
        )          
       
        super.onCreate(savedInstanceState)
        
        usbManager =
            getSystemService(
                Context.USB_SERVICE
            ) as UsbManager
            
        usbDeviceProvider =
            AndroidUsbDeviceProvider(
                usbManager
            )
        
        holfuyDevice =
            RealHolfuyDevice(
                usbManager,
                usbDeviceProvider
            )
               
        registerReceivers()

        permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(packageName)
            },
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            HolfuyConfigToolTheme {
                
                val factory = remember {
                    MainViewModelFactory(
                        holfuyDevice
                    )
                }

                val viewModel: MainViewModel = viewModel(
                    factory = factory
                )
                mainViewModel = viewModel
                
                val firmwarePicker =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                
                        if (uri == null) {
                            return@rememberLauncherForActivityResult
                        }
                
                        val bytes =
                            contentResolver
                                .openInputStream(uri)
                                ?.readBytes()
                                ?: return@rememberLauncherForActivityResult
                
                        val fileName =
                            getDisplayName(
                                contentResolver,
                                uri
                            )
                
                        Log.i(
                            TAG,
                            "Loaded firmware: $fileName (${bytes.size} bytes)"
                        )
                
                        viewModel.setFirmware(
                            fileName,
                            bytes
                        )
                    } 
                    
                val deviceState by viewModel.deviceStateFlow.collectAsState()
                
                var showHelp by remember {
                    mutableStateOf(false)
                }
                
                if (showHelp) {
                
                    HelpScreen(
                        onBack = {
                            showHelp = false
                        }
                    )
                
                } else {
                
                    MainScreen(
                        uiState = viewModel.uiState,
                        deviceState = deviceState,
                
                        onConnectClick = {
                            viewModel.connect()
                        },
                
                        onSelectFirmwareClick = {
                
                            firmwarePicker.launch("*/*")
                
                        },
                
                        onUpdateFirmwareClick =
                            viewModel::updateFirmware,
                
                        onHelpClick = {
                            showHelp = true
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy()
    {

        Log.i(
            TAG,
            "onDestroy changingConfigurations=$isChangingConfigurations"
        )
        
        unregisterReceivers()
        
        super.onDestroy()
    }
    
    override fun onResume()
    {
        super.onResume()
    
        val usbDevice =
            usbManager.deviceList
                .values
                .firstOrNull()
    
        val permissionGranted =
            usbDevice != null &&
            usbManager.hasPermission(
                usbDevice
            )
    
        DeviceRepository.setPermissionGranted(
            permissionGranted
        )
    }
    
    override fun onConfigurationChanged(
        newConfig: Configuration
    )
    {
        super.onConfigurationChanged(newConfig)
    
        Log.i(
            TAG,
            "onConfigurationChanged keyboard=${newConfig.keyboard} " +
            "hardKeyboardHidden=${newConfig.hardKeyboardHidden} " +
            "navigation=${newConfig.navigation}"
        )
    }
    
    override fun onPause()
    {
        Log.i(
            TAG,
            "onPause"
        )
    
        super.onPause()
    }
    
    override fun onStop()
    {
        Log.i(
            TAG,
            "onStop"
        )
    
        super.onStop()
    }
    
    override fun onStart()
    {
        super.onStart()
    
        Log.i(
            TAG,
            "onStart"
        )
    }
}