package com.holfuy.configtool.protocol

import android.annotation.SuppressLint
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.holfuy.configtool.usb.HolfuyUsb
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class NulinkInterfaceType(val value: Byte)
{
    USB(0x00)
}

@Suppress("ArrayInDataClass")
data class ConnectResult(
    val buffer: ByteArray?,
    val isChecksum: Boolean,
    val isTimeout: Boolean
)

@Suppress("ArrayInDataClass")
data class CommandResult(
    val buffer: ByteArray?,
    val isChecksum: Boolean
)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object ISPManager {

    private var read_endpoint_index = 0
    private var write_endpoint_index = 1
    private var connect_interface_index = 0
    private val forceClaim = true
    private val TAG = "HolfuyUSB-ISP"
    private val TAGP = "HolfuyUSB-Pkt"

    public var packetNumber: UInt = (0x00000005).toUInt()
    public var interfaceType : NulinkInterfaceType = NulinkInterfaceType.USB
    
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var readEndpoint: UsbEndpoint? = null
    private var writeEndpoint: UsbEndpoint? = null

    fun sendCMD_UPDATE_BIN(cmd: ISPCommands ,sendByteArray:ByteArray,startAddress:UInt, callback: ((ByteArray?, Int) -> Unit)) {

        if(cmd != ISPCommands.CMD_UPDATE_APROM && cmd != ISPCommands.CMD_UPDATE_DATAFLASH){
            return
        }
        
        if (sendByteArray.size < 48) {
            Log.w(
                TAG,
                "UPDATE_BIN rejected: firmware is too small (${sendByteArray.size} bytes)"
            )
            callback.invoke(null, -1)
            return
        }

        var firstData = byteArrayOf()
        // First packet carries 48 bytes of firmware payload.
        for (i in 0..47){
            firstData = firstData + sendByteArray[i]
        }
        var remainDataList: List<ByteArray> = listOf()
        val remainData = sendByteArray.copyOfRange(48,sendByteArray.lastIndex+1)
        var index = 0
        var dataArray = byteArrayOf()
        // Remaining packets carry 56-byte payload chunks.
        for (byte in remainData){
            dataArray = dataArray + byte
            index = index + 1

            if(index == 56){
                index = 0
                remainDataList = remainDataList + dataArray.clone()
                dataArray = byteArrayOf()
            }
        }
        if(dataArray.isNotEmpty()){
            //還有剩
                for(i in dataArray.size+1..56){
                    dataArray = dataArray + 0x00
                }

            if(dataArray.size == 56){
                remainDataList = remainDataList + dataArray.clone()
            }

        }
        Log.d(TAG, "CMD_UPDATE   CMD:"+cmd.toString()+"  size:"+sendByteArray.size+"  allPackNum:"+dataArray.size+1)
        var sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd, packetNumber , startAddress , sendByteArray.size , firstData , true)
        this.write(sendBuffer)        
        var readBuffer = waitForExpectedPacket(packetNumber + 1u, timeoutMs = 20000)
        if (readBuffer == null) {
            Log.i(
                TAG,
                "UPDATE_BIN timeout waiting for packet ${packetNumber + 1u}"
            )
            callback.invoke(null, -1)
            return
        }       
        Log.d(
            TAG,
            "UPDATE_BIN first block expected=${packetNumber + 1u} actual=${
                if (readBuffer != null)
                    ISPCommandTool.toPackNo(readBuffer)
                else
                    "null"
            }"
        )
        var isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)

        callback.invoke(readBuffer, 0) //5% 起跳

        if(!isChecksum){
            callback.invoke(readBuffer, -1)
            return
        }

        for (i in 0..remainDataList.size-1){
            sendBuffer = ISPCommandTool.toUpdataBin_CMD(cmd, packetNumber , startAddress , sendByteArray.size , remainDataList[i] , false)
            this.write(sendBuffer)        
            readBuffer = waitForExpectedPacket(packetNumber + 1u, timeoutMs = 20000)   
            if (readBuffer == null) {
                Log.i(
                    TAG,
                    "UPDATE_BIN timeout waiting for packet ${packetNumber + 1u}"
                )
                callback.invoke(null, -1)
                return
            }     
            Log.d(
                TAG,
                "UPDATE_BIN block=$i expected=${packetNumber + 1u} actual=${
                    if (readBuffer != null)
                        ISPCommandTool.toPackNo(readBuffer)
                    else
                        "null"
                }"
            )
            isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
            if(!isChecksum){
                callback.invoke(readBuffer, -1)
                return
            }
            val progress = ((i + 1) * 100) / remainDataList.size
            callback.invoke(
                readBuffer,
                progress
            )
        }
        callback.invoke(readBuffer, 100)
    }


    fun sendCMD_ERASE_ALL( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_ERASE_ALL
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)

        this.write( sendBuffer)
        val readBuffer = this.read()
        val isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)        
        Log.d(
            TAG,
            "ERASE_ALL ackPackNo=${readBuffer?.let { ISPCommandTool.toPackNo(it) } ?: "null"} checksum=$isChecksum"
        )

        callback.invoke(readBuffer, isChecksum)
    }

    fun sendCMD_READ_CONFIG( callback: ((ByteArray?) -> Unit)) {

        val cmd = ISPCommands.CMD_READ_CONFIG       
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)        
        Log.d(TAG, "sendCMD cmd=$cmd packetNumber=$packetNumber")        
        this.write(sendBuffer)        
        val expectedPackNo = packetNumber + 1.toUInt()        
        val readBuffer = waitForExpectedPacket(expectedPackNo)        
        if (readBuffer == null) {        
            Log.i(TAG, "READ_CONFIG readBuffer == null")        
            callback.invoke(null)        
            return
        }        
        this.isChecksum_PackNo(sendBuffer, readBuffer)        
        callback.invoke(readBuffer)
    }
    
    suspend fun suspendCMD_READ_CONFIG(): ByteArray? =
        suspendCancellableCoroutine { cont ->
    
            sendCMD_READ_CONFIG { buffer ->
    
                if (cont.isActive) {
                    cont.resume(buffer)
                }
            }
        }

    fun sendCMD_GET_FWVER(callback: ((ByteArray?, Boolean) -> Unit)) {
        
        val cmd = ISPCommands.CMD_GET_FWVER
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        Log.d(TAG, "sendCMD cmd=${cmd} packetNumber=$packetNumber")
        this.write( sendBuffer)
        val expectedPackNo = packetNumber + 1.toUInt()        
        val readBuffer = waitForExpectedPacket(expectedPackNo)
        val isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
        callback.invoke(readBuffer, isChecksum)
    }
    
    suspend fun suspendCMD_GET_FWVER(): CommandResult =
        suspendCancellableCoroutine { cont ->
    
            sendCMD_GET_FWVER { buffer, isChecksum ->
    
                if (cont.isActive) {
                    cont.resume(
                        CommandResult(
                            buffer,
                            isChecksum
                        )
                    )
                }
            }
        }

    fun sendCMD_RUN_APROM( callback: ((Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_RUN_APROM
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        this.write( sendBuffer)

        callback.invoke(true)
    }

    fun sendCMD_UPDATE_CONFIG(config0: UInt,config1: UInt,config2: UInt,config3: UInt, callback: ((ByteArray?) -> Unit)) {

        sendCMD_ERASE_ALL { readArray, isChackSum ->

            if (!isChackSum) return@sendCMD_ERASE_ALL

            //config＿1  先寫死
            ISPCommands.CMD_UPDATE_CONFIG
            val sendBuffer = ISPCommandTool.toUpdataCongigeCMD(config0, config1, config2,config3, packetNumber)
            this.write( sendBuffer)

            val readBuffer = this.read()
            this.isChecksum_PackNo(sendBuffer, readBuffer)

            callback.invoke(readBuffer)
        }

    }

    fun sendCMD_SYNC_PACKNO(
        callback: ((ByteArray?, Boolean) -> Unit)
    ) {
    
        val cmd = ISPCommands.CMD_SYNC_PACKNO    
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)    

        // SYNC requires duplicated packet number in bytes 8-11.
        val packNoBytes = HEXTool.UIntTo4Bytes(packetNumber)    
        System.arraycopy(packNoBytes, 0, sendBuffer, 8, 4)    
        Log.d(
            TAG,
            "sendCMD cmd=$cmd " +
            "packetNumber=$packetNumber " +
            "syncPackNo=$packetNumber"
        )    
        this.write(sendBuffer)    
        val expectedPackNo = packetNumber + 1.toUInt()    
        val readBuffer = waitForExpectedPacket(expectedPackNo)    
        val isChecksum = this.isChecksum_PackNo(
            sendBuffer,
            readBuffer
        )    
        callback.invoke(readBuffer, isChecksum)
    }
    
    suspend fun suspendCMD_SYNC_PACKNO(): CommandResult =
        suspendCancellableCoroutine { cont ->
    
            sendCMD_SYNC_PACKNO { buffer, isChecksum ->
    
                if (cont.isActive) {
                    cont.resume(
                        CommandResult(
                            buffer,
                            isChecksum
                        )
                    )
                }
            }
        }

    fun sendCMD_CONNECT(callback: ((ByteArray?, Boolean, Boolean) -> Unit)) {
 
        executeConnect { readBuffer, isTimeout ->
            val sendBuffer =
                ISPCommandTool.toCMD(
                    ISPCommands.CMD_CONNECT,
                    1.toUInt()
                )
            val isChecksum =
                this.isChecksum_PackNo(
                    sendBuffer,
                    readBuffer
                )
            callback.invoke(
                readBuffer,
                isChecksum,
                isTimeout
            )
        }
    }
  
    @SuppressLint("NewApi")
    private fun executeConnect(callback: (ByteArray?, Boolean) -> Unit) {    
        if (
            usbConnection == null ||
            readEndpoint == null ||
            writeEndpoint == null
        ) {        
            Log.i(TAG, "executeConnect: USB session not open")        
            callback.invoke(null, true)        
            return
        }
        
        val connection = usbConnection!!
        val writePoint = writeEndpoint!!
        val readPoint = readEndpoint!!        
        val readBuffer = ByteArray(64)    
        var index = 0
        
        // Send CONNECT packets until a non-zero response is received,
        // indicating the device has transitioned into ISP mode.  
        while (index < 20) {    
            packetNumber = 1.toUInt()    
            val sendBuffer =
                ISPCommandTool.toCMD(
                    ISPCommands.CMD_CONNECT,
                    packetNumber
                )    
            Log.d(
                TAG,
                "sendCMD cmd=CMD_CONNECT packetNumber=$packetNumber"
            )    
            val sendBufferString = HEXTool.toHexString(sendBuffer)    
            var display = HEXTool.toDisPlayString(sendBufferString)    
            val isWrite =
                connection.bulkTransfer(
                    writePoint,
                    sendBuffer,
                    sendBuffer.size,
                    0
                )    
            Log.d(TAGP, "isWrite=$isWrite    ,sendBuffer:  $display")    
            readBuffer.fill(0)    
            val isRead =
                connection.bulkTransfer(
                    readPoint,
                    readBuffer,
                    readBuffer.size,
                    100
                )    
            val readBufferString = HEXTool.toHexString(readBuffer)    
            display = HEXTool.toDisPlayString(readBufferString)
            Log.d(TAGP, "isRead=$isRead    ,readBuffer:  $display")    
            if (isRead <= 0) {    
                Thread.sleep(200)    
                index++    
                Log.d(TAG, "index=$index")
                continue
            }    
            val allZero = readBuffer.all { it == 0.toByte() }    
            if (!allZero) {    
                Log.i(TAG, "Holfuy entered ISP mode")    
                callback.invoke(readBuffer, false)    
                return
            }    
            Thread.sleep(200)    
            index++    
            Log.d(TAG, "index=$index")
        }
        closeUsbSession()
        callback.invoke(null, true)
    }
    
    suspend fun suspendCMD_CONNECT(): ConnectResult =
        suspendCancellableCoroutine { cont ->
    
            sendCMD_CONNECT { buffer, isChecksum, isTimeout ->
    
                if (cont.isActive) {
                    cont.resume(
                        ConnectResult(
                            buffer,
                            isChecksum,
                            isTimeout
                        )
                    )
                }
            }
        }

    @SuppressLint("NewApi")
    fun openUsbSession(
        usbManager: UsbManager,
        usbDevice: UsbDevice
    ): Boolean {
    
        Log.i(TAG, "Opening USB session")
    
        if (!HolfuyUsb.isSupported(usbDevice)) {
    
            Log.w(
                TAG,
                "Unsupported USB device productId=0x${usbDevice.productId.toString(16)}"
            )
    
            return false
        }
    
        if (
            usbConnection != null &&
            readEndpoint != null &&
            writeEndpoint != null
        ) {
    
            Log.i(TAG, "USB session already open")
    
            return true
        }
    
        connect_interface_index = 0
    
        usbInterface =
            usbDevice.getInterface(connect_interface_index)
    
        readEndpoint =
            usbInterface!!.getEndpoint(read_endpoint_index)
    
        writeEndpoint =
            usbInterface!!.getEndpoint(write_endpoint_index)
    
        usbConnection =
            usbManager.openDevice(usbDevice)
    
        if (usbConnection == null) {
    
            Log.i(
                TAG,
                "USB session open failed: usbConnection == null"
            )
    
            return false
        }
    
        val claimed = usbConnection!!.claimInterface(
            usbInterface,
            forceClaim
        )

        Log.i(
            TAG,
            "USB session open, claimInterface=$claimed"
        )

        if (!claimed) {
            closeUsbSession()
            return false
        }

        return true
    }
    
    fun closeUsbSession() {  
        Log.i(TAG, "closeUsbSession requested")  
        try {    
            usbConnection?.releaseInterface(usbInterface)    
        } catch (_: Exception) {
        }
    
        try {    
            usbConnection?.close()    
        } catch (_: Exception) {
        }
    
        usbConnection = null
        usbInterface = null
        readEndpoint = null
        writeEndpoint = null    
        Log.i(TAG, "closeUsbSession, USB session closed")
    }

    fun sendCMD_GET_DEVICEID( callback: ((ByteArray?, Boolean) -> Unit)) {

        val cmd = ISPCommands.CMD_GET_DEVICEID
        val sendBuffer = ISPCommandTool.toCMD(cmd, packetNumber)
        Log.d(TAG, "sendCMD cmd=${cmd} packetNumber=$packetNumber")  
        this.write( sendBuffer)
        val expectedPackNo = packetNumber + 1.toUInt()
        val readBuffer = waitForExpectedPacket(expectedPackNo)
        val isChecksum = this.isChecksum_PackNo(sendBuffer, readBuffer)
        callback.invoke(readBuffer,isChecksum)
    }
    
    suspend fun suspendCMD_GET_DEVICEID(): CommandResult =
        suspendCancellableCoroutine { cont ->
    
            sendCMD_GET_DEVICEID { buffer, isChecksum ->
    
                if (cont.isActive) {
                    cont.resume(
                        CommandResult(
                            buffer,
                            isChecksum
                        )
                    )
                }
            }
        }

    public fun isChecksum_PackNo(sendBuffer: ByteArray, readBuffer: ByteArray?): Boolean {

        if (readBuffer == null) {
            Log.i("isChecksum_PackNo", "readBuffer == null")
            return false
        }

        // checksum
        val checksum = ISPCommandTool.toChecksumBySendBuffer(sendBuffer)
        val resultChecksum = ISPCommandTool.toChecksumByReadBuffer(readBuffer)
        Log.d("isChecksum_PackNo", "computedChecksum=$checksum resultChecksum=$resultChecksum")
        val sendDisplay = HEXTool.toDisPlayString(HEXTool.toHexString(sendBuffer))
        Log.d("isChecksum_PackNo", "checksumSendBuffer: $sendDisplay")
        if (checksum != resultChecksum) {
            Log.d(
                "isChecksum_PackNo",
                "checksum $checksum != resultChecksum $resultChecksum"
            )
            return false
        }
        
        // packet number
        val packNo = packetNumber + (0x00000001).toUInt()
        val resultPackNo = ISPCommandTool.toPackNo(readBuffer)
        
        if (packNo != resultPackNo) {
            Log.d(
                "isChecksum_PackNo",
                "packNo $packNo != resultPackNo $resultPackNo"
            )
            return false
        }
        packetNumber = packNo + (0x00000001).toUInt()
        Log.d(
            "isChecksum_PackNo",
            "packNo $packNo == resultPackNo $resultPackNo ,checksum $checksum == resultChecksum $resultChecksum"
        )
        return true
    }

    @SuppressLint("NewApi")
    private fun read(): ByteArray? {    
        val connection = usbConnection
        val endpoint = readEndpoint    
        if (connection == null || endpoint == null) {    
            Log.i(TAG, "read failed: USB session not open")    
            return null
        }    
        val readBuffer = ByteArray(64)    
        val i =
            connection.bulkTransfer(
                endpoint,
                readBuffer,
                readBuffer.size,
                100
            )    
        val readBufferString = HEXTool.toHexString(readBuffer)    
        val display = HEXTool.toDisPlayString(readBufferString)    
        Log.d(TAGP, "i=$i    ,readBuffer:  $display")    
        if (i <= 0) {
            return null
        }    
        return readBuffer
    }
    
    // Since the device uses USB HID semantics, each response to a write
    // command remains available for subsequent read commands until another
    // write occurs. The first read after a write frequently returns the
    // response to the previous command rather than the current command.
    //
    // This function continuously reads packets until either:
    //   1. A packet with the expected packet number is received, or
    //   2. The timeout expires.
    //
    // Packets with unexpected packet numbers are logged and discarded.
    private fun waitForExpectedPacket(
        expectedPackNo: UInt,
        timeoutMs: Long = 2000
    ): ByteArray? {
    
        val start = SystemClock.elapsedRealtime()
        var zeroPacketCount = 0
    
        while ((SystemClock.elapsedRealtime() - start) < timeoutMs) {
    
            val readBuffer = this.read()
    
            if (readBuffer == null) {
                Log.d(TAG, "waitForExpectedPacket readBuffer == null")
                continue
            }
    
            val resultPackNo = ISPCommandTool.toPackNo(readBuffer)
            val resultChecksum = ISPCommandTool.toChecksumByReadBuffer(readBuffer)
    
            if (resultPackNo != expectedPackNo) {
                if (resultPackNo == 0u) {
                    zeroPacketCount++
                    continue
                }                
                val readBufferString = HEXTool.toHexString(readBuffer)
                val display = HEXTool.toDisPlayString(readBufferString)

                Log.d(
                    TAG,
                    "waitForExpectedPacket " +
                    "resultPackNo=$resultPackNo " +
                    "expectedPackNo=$expectedPackNo " +
                    "checksum=$resultChecksum"
                )  

                Log.d(
                    TAGP,
                    "Ignoring unexpected packet: $display"
                )
                continue
            }
            
            if (zeroPacketCount > 0) {
                Log.d(
                    TAG, 
                    "Ignored $zeroPacketCount zero packets while waiting for packNo=$expectedPackNo"
                )
            }
         
            return readBuffer
        }
    
        Log.i(
            TAG,
            "waitForExpectedPacket timeout waiting for packNo=$expectedPackNo"
        )    
        return null
    }
    
    @SuppressLint("NewApi")
    private fun write(cmdArray: ByteArray)
    {
        val connection = usbConnection
        val endpoint = writeEndpoint
    
        if (connection == null || endpoint == null) {
    
            Log.i(
                TAG,
                "write failed: USB session not open"
            )
    
            return
        }
    
        cmdArray[1] = interfaceType.value
    
        val sendBufferString =
            HEXTool.toHexString(cmdArray)
    
        val display =
            HEXTool.toDisPlayString(sendBufferString)
    
        val i =
            connection.bulkTransfer(
                endpoint,
                cmdArray,
                cmdArray.size,
                100
            )
    
        Log.d(
            TAGP,
            "i=$i, writeBuffer: $display"
        )
    }
}