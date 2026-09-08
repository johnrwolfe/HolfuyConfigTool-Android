package com.holfuy.configtool.device

import android.hardware.usb.UsbManager
import android.util.Log
import com.holfuy.configtool.diagnostics.DiagnosticLogger
import com.holfuy.configtool.protocol.ISPCommands
import com.holfuy.configtool.protocol.ISPManager
import com.holfuy.configtool.usb.UsbDeviceProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RealHolfuyDevice(
    private val usbManager: UsbManager,
    private val usbDeviceProvider: UsbDeviceProvider,
    private val diagnosticLogger: DiagnosticLogger
) : HolfuyDevice {

    private val operationMutex = Mutex()
    private val sessionLock = Any()
    private var sessionGeneration = 0L

    override suspend fun connect(): Boolean = operationMutex.withLock {
        var connectionSucceeded = false

        val operationGeneration = synchronized(sessionLock) {
            sessionGeneration
        }

        try {
            val usbDevice = usbDeviceProvider.findDevice()
                ?: run {
                    diagnosticLogger.recordIspConnectionFailed(
                        "No supported USB device"
                    )
                    return@withLock false
                }

            Log.d(
                TAG,
                "Using USB device: ${usbDevice.deviceName}"
            )

            synchronized(sessionLock) {
                if (sessionGeneration != operationGeneration) {
                    Log.i(
                        TAG,
                        "Connect aborted because USB session was invalidated"
                    )

                    diagnosticLogger.recordIspConnectionFailed(
                        "USB session invalidated"
                    )

                    return@withLock false
                }
            }

            if (!ISPManager.openUsbSession(usbManager, usbDevice)) {
                Log.e(
                    TAG,
                    "openUsbSession failed"
                )

                diagnosticLogger.recordIspConnectionFailed(
                    "Unable to open USB session"
                )

                return@withLock false
            }

            diagnosticLogger.recordUsbSessionOpened()

            val connectResult =
                ISPManager.suspendCMD_CONNECT()

            if (connectResult.isTimeout) {
                Log.e(
                    TAG,
                    "CMD_CONNECT timeout"
                )

                diagnosticLogger.recordIspConnectionFailed(
                    "CMD_CONNECT timeout"
                )

                return@withLock false
            }

            if (!connectResult.isChecksum) {
                Log.e(
                    TAG,
                    "CMD_CONNECT checksum failure"
                )

                diagnosticLogger.recordIspConnectionFailed(
                    "CMD_CONNECT checksum failure"
                )

                return@withLock false
            }

            val syncResult =
                ISPManager.suspendCMD_SYNC_PACKNO()

            if (!syncResult.isChecksum) {
                Log.e(
                    TAG,
                    "CMD_SYNC_PACKNO checksum failure"
                )

                diagnosticLogger.recordIspConnectionFailed(
                    "CMD_SYNC_PACKNO checksum failure"
                )

                return@withLock false
            }

            synchronized(sessionLock) {
                if (sessionGeneration != operationGeneration) {
                    Log.i(
                        TAG,
                        "Connect completed after USB session was invalidated"
                    )

                    diagnosticLogger.recordIspConnectionFailed(
                        "USB session invalidated"
                    )

                    return@withLock false
                }

                connectionSucceeded = true

                diagnosticLogger.recordIspConnected()

                true
            }
        }
        catch (e: Exception) {
            diagnosticLogger.recordUnexpectedException(
                "ISP connection",
                e
            )

            throw e
        }
        finally {
            synchronized(sessionLock) {
                if (sessionGeneration == operationGeneration) {
                    DeviceRepository.setConnected(
                        connectionSucceeded
                    )
                }
            }
        }
    }

    override suspend fun updateFirmware(
        firmwareBytes: ByteArray,
        onProgress: (Int) -> Unit
    ): Boolean = operationMutex.withLock {
    
        val operationGeneration =
            synchronized(sessionLock) {
                sessionGeneration
            }
    
        Log.i(
            TAG,
            "Starting firmware update (${firmwareBytes.size} bytes)"
        )
    
        diagnosticLogger.recordFirmwareUpdateStarted(
            firmwareBytes.size
        )
    
        var success = true
        var lastDiagnosticProgress = 0
    
        ISPManager.sendCMD_UPDATE_BIN(
            ISPCommands.CMD_UPDATE_APROM,
            firmwareBytes,
            0u
        ) { _, progress ->
    
            if (progress >= 0) {
                onProgress(progress)
            }
    
            if (
                progress >= 25 &&
                progress >= lastDiagnosticProgress + 25
            ) {
                lastDiagnosticProgress =
                    (progress / 25) * 25
    
                diagnosticLogger.recordFirmwareUpdateProgress(
                    lastDiagnosticProgress
                )
            }
    
            if (progress < 0) {
                success = false
            }
        }
    
        val sessionInvalidated =
            synchronized(sessionLock) {
                sessionGeneration != operationGeneration
            }
    
        if (sessionInvalidated) {
    
            Log.i(
                TAG,
                "Firmware update stopped because USB session was invalidated"
            )
    
            return@withLock false
        }
    
        Log.i(
            TAG,
            "Firmware update finished success=$success"
        )
    
        if (success) {
            diagnosticLogger.recordFirmwareUpdateCompleted()
        } else {
            diagnosticLogger.recordFirmwareUpdateFailed(
                "ISP firmware update reported failure"
            )
        }
    
        return@withLock success
    }

    override fun onUsbDetached() {
        Log.i(
            TAG,
            "onUsbDetached()"
        )

        synchronized(sessionLock) {
            sessionGeneration++
        }

        ISPManager.closeUsbSession()
    }

    companion object {
        private const val TAG = "HolfuyDevice"
    }
}
