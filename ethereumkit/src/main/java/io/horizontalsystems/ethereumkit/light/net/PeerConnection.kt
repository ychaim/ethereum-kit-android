package io.horizontalsystems.ethereumkit.light.net;

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.net.message.StaticMessages
import io.horizontalsystems.ethereumkit.light.net.p2p.DisconnectMessage
import io.horizontalsystems.ethereumkit.light.net.p2p.HelloMessage
import io.horizontalsystems.ethereumkit.light.net.p2p.P2pMessageCodes
import io.horizontalsystems.ethereumkit.light.net.rlpx.EncryptionHandshake
import io.horizontalsystems.ethereumkit.light.net.rlpx.FrameCodec
import io.horizontalsystems.ethereumkit.light.util.ByteUtil.bigEndianToShort
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom


class PeerConnection {
    companion object {


        fun run() {
            val socket = Socket()
            socket.connect(InetSocketAddress("52.232.243.152", 30303), 10000)
            socket.soTimeout = 10000

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            try {
                println("Socket connected.")

                val sr = SecureRandom()

                val myKey = ECKey(sr)

                val handshake = EncryptionHandshake(ECKey.fromNodeId("6332792c4a00e3e4ee0926ed89e0d27ef985424d97b6a45bf0f23e51f0dcb5e66b875777506458aea7af6f9e4ffb69f43f3778ee73c81ed9d34c51c4b16b0b0f".hexStringToByteArray()).pubKeyPoint)
                val staticMessages = StaticMessages()

                //auth
                val initiateMessage = handshake.createAuthInitiateV4(myKey)
                println(initiateMessage.toString())
                val initiatePacket = handshake.encryptAuthInitiateV4(initiateMessage)
                output.write(initiatePacket)

                //ack-auth
                val responsePacket = readEIP8Packet(input)
                val initiateMessageResponse = handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket)
                print(initiateMessageResponse.toString())

                //hello
                val frameCodec = FrameCodec(handshake.secrets)
                val helloMessage = staticMessages.createHelloMessage(myKey.nodeId.toHexString())
                frameCodec.writeFrame(FrameCodec.Frame(helloMessage.code.toInt(), helloMessage.encoded), output)

                //hello response
                val frames = frameCodec.readFrames(input)
                if (frames == null || frames.isEmpty())
                    return
                val frame = frames[0]
                val payload = frame.stream.readBytes()

                if (frame.type == P2pMessageCodes.HELLO.asByte().toLong()) {
                    val responseHello = HelloMessage(payload)
                    println(responseHello.toString())

                    responseHello.capabilities.forEach {
                        println("supported capability = $it")
                    }
                } else {
                    val disconnectMessage = DisconnectMessage(payload)
                    println(disconnectMessage.toString())

//                    channel.getNodeStatistics().nodeDisconnectedRemote(message.getReason())
                }


                while (true) {
                    val frames = frameCodec.readFrames(input)


                }


            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                input.close()
                output.close()
                socket.close()
            }
        }


        private fun readEIP8Packet(input: InputStream): ByteArray? {

            val prefix = ByteArray(2)
            input.read(prefix)
            val size = bigEndianToShort(prefix)
            println("size = $size")
            val messagePackets = ByteArray(size.toInt())

            input.read(messagePackets)

            val fullResponse = ByteArray(size + 2)
            System.arraycopy(prefix, 0, fullResponse, 0, 2)
            System.arraycopy(messagePackets, 0, fullResponse, 2, size.toInt())

            return fullResponse
        }
    }


}
