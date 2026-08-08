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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import com.holfuy.configtool.device.DeviceRepository
import com.holfuy.configtool.device.HolfuyDevice
import com.holfuy.configtool.device.RealHolfuyDevice
import com.holfuy.configtool.firmware.FirmwareRepository
import com.holfuy.configtool.firmware.RepositoryStorage
import com.holfuy.configtool.firmware.RefreshResult
import com.holfuy.configtool.ui.screens.HelpScreen
import com.holfuy.configtool.ui.screens.MainScreen
import com.holfuy.configtool.ui.screens.RepositoryConfigurationScreen
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
    private lateinit var activityViewModel: MainViewModel
    private lateinit var usbManager: UsbManager
    private lateinit var holfuyDevice: HolfuyDevice
    private lateinit var usbDeviceProvider: UsbDeviceProvider
    private lateinit var firmwareRepository: FirmwareRepository
    private lateinit var repositoryStorage: RepositoryStorage
    
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
    
    private fun formatInstant(
        instant: Instant
    ): String
    {
        return DateTimeFormatter
            .ofLocalizedDateTime(
                FormatStyle.MEDIUM,
                FormatStyle.SHORT
            )
            .withZone(ZoneId.systemDefault())
            .format(instant)
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
    
    // USB permission acquisition is intentionally initiated by the Connect 
    // action rather than by the attach broadcast. This provides a consistent 
    // workflow regardless of whether the app is already running when the station 
    // is attached, whether the permission dialog was previously dismissed, or 
    // how individual Android versions deliver USB lifecycle events.    
    private fun connectOrRequestPermission()
    {
        if (ensureUsbPermission()) {
    
            activityViewModel.connect()
        }
    }
    
    private fun findSupportedUsbDevice(): UsbDevice?
    {
        return usbManager.deviceList
            .values
            .firstOrNull {
                HolfuyUsb.isSupported(it)
            }
    }
    
    private fun refreshUsbState()
    {
        val usbDevice =
            findSupportedUsbDevice()
    
        DeviceRepository.setAttached(
            usbDevice != null
        )
    
        DeviceRepository.setPermissionGranted(
            usbDevice?.let {
                usbManager.hasPermission(it)
            } ?: false
        )
    
        if (usbDevice == null) {
            DeviceRepository.clearConnectionState()
        }
    }
    
    // true = permission already granted
    // false = permission not granted yet but has been requested 
    //         if a supported device is attached
    private fun ensureUsbPermission(): Boolean
    {
        val usbDevice =
            findSupportedUsbDevice()
                ?: return false
    
        if (usbManager.hasPermission(usbDevice)) {
    
            Log.i(
                TAG,
                "USB permission already granted"
            )
    
            DeviceRepository.setPermissionGranted(true)
    
            return true
        }
    
        Log.i(
            TAG,
            "Requesting USB permission"
        )
    
        usbManager.requestPermission(
            usbDevice,
            permissionIntent
        )
    
        return false
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
                
                if (granted) {
                
                    activityViewModel.connect()
                }
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
    
                intent.getSupportedUsbDevice()
                    ?: return
    
                Log.i(
                    TAG,
                    "Supported USB device attached"
                )
    
                refreshUsbState()
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
                
                activityViewModel.clearTransientStatus()
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
        
        repositoryStorage = RepositoryStorage(this)
        
        firmwareRepository = FirmwareRepository(repositoryStorage)
               
        registerReceivers()

        permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(packageName)
            },
            PendingIntent.FLAG_MUTABLE
        )
        
        refreshUsbState()

        val factory =
            MainViewModelFactory(
                holfuyDevice
            )
        
        activityViewModel =
            ViewModelProvider(
                this,
                factory
            )[MainViewModel::class.java]

        setContent {
            HolfuyConfigToolTheme {
                
                val viewModel = activityViewModel
                             
                val firmwareFolderPicker =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri: Uri? ->
                
                        if (uri == null)
                            return@rememberLauncherForActivityResult
                
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                
                        firmwareRepository.configure(uri)
                
                        Log.i(
                            TAG,
                            "Firmware folder selected: $uri"
                        )
                    }
                              
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
                
                var showHelp by rememberSaveable {
                    mutableStateOf(false)
                }
                
                if (viewModel.uiState.configuringRepository) {
                
                    RepositoryConfigurationScreen(
                        onContinue = {
                    
                            activityViewModel.endRepositoryConfiguration()
                    
                            firmwareFolderPicker.launch(null)
                        }
                    )
                
                } else if (showHelp) {
                
                    HelpScreen(
                        onBack = {
                            showHelp = false
                        }
                    )
                
                } else {
                
                    MainScreen(
                        uiState = viewModel.uiState,
                        deviceState = deviceState,
                        
                        onConnectClick =
                            ::connectOrRequestPermission,
                            
                        onSelectFirmwareClick = {                        
                            activityViewModel.clearTransientStatus()                
                            firmwarePicker.launch("*/*")                
                        },
                                            
                        onUpdateFirmwareClick =
                            viewModel::updateFirmware,
                            
                        onHelpClick = {
                            showHelp = true
                        },
                        
                        repositoryDisplayName = firmwareRepository.displayName,
                        
                        onRefreshRepositoryClick = {
                            lifecycleScope.launch {
                        
                                viewModel.setRefreshResult(null)
                        
                                viewModel.setRefreshResult(
                                    firmwareRepository.refresh()
                                )
                            }
                        },

                        lastVerifiedText =
                            viewModel.uiState.refreshResult
                                ?.verifiedAt
                                ?.let(::formatInstant)
                    )
                }
            }
        }
    }
    
    override fun onDestroy()
    {

        Log.d(
            TAG,
            "onDestroy changingConfigurations=$isChangingConfigurations"
        )
        
        unregisterReceivers()
        
        super.onDestroy()
    }
    
    override fun onResume()
    {
        super.onResume()
    
        refreshUsbState()
    
        Log.i(
            TAG,
            "onResume attached=${DeviceRepository.stateFlow.value.attached} permissionGranted=${DeviceRepository.stateFlow.value.permissionGranted}"
        )
    
        if (!firmwareRepository.configured) {
    
            activityViewModel.beginRepositoryConfiguration()
    
        } else {
    
            Log.i(
                TAG,
                "Firmware repository: ${firmwareRepository.displayName}"
            )
            
            lifecycleScope.launch {
            
                activityViewModel.setRefreshResult(null)
            
                activityViewModel.setRefreshResult(
                    firmwareRepository.refresh()
                )
            }
        }
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
}