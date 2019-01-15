package io.horizontalsystems.ethereumkit.light.net;

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.net.message.*
import io.horizontalsystems.ethereumkit.light.net.p2p.DisconnectMessage
import io.horizontalsystems.ethereumkit.light.net.p2p.HelloMessage
import io.horizontalsystems.ethereumkit.light.net.p2p.P2pMessageCodes
import io.horizontalsystems.ethereumkit.light.net.rlpx.EncryptionHandshake
import io.horizontalsystems.ethereumkit.light.net.rlpx.FrameCodec
import io.horizontalsystems.ethereumkit.light.util.ByteUtil.bigEndianToShort
import java.io.InputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.*


class PeerConnection {
    companion object {


        fun run() {
            val socket = Socket()
            socket.connect(InetSocketAddress("192.168.31.106", 30303), 10000)
            socket.soTimeout = 0

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            val genesisBlockHash = "41941023680923e0fe4d74a34bdac8141f2540e3ae90623718e47d66d1ca4a2d".hexStringToByteArray()
            val address = "0xF757461bdc25Ee2b047d545a50768e52D530b750"
            val lastBlockNumber = 4848254
            val lastBlockhash = "0xcfbbc6a17c40076f24ee926fcb644964a077edb63dc1c6742511001513e73d05"
            val lastBlockHeaderStateRootHash = "0xea541a1fd39ab125237bf4cb9d5d0e27c0827452bddac6392c5072ce27b0c423"

            try {
                println("Socket connected.")

                val sr = SecureRandom()

                val myKey = ECKey(sr)

                val handshake = EncryptionHandshake(ECKey.fromNodeId("e679038c2e4f9f764acd788c3935cf526f7f630b55254a122452e63e2cfae3066ca6b6c44082c2dfbe9ddffc9df80546d40ef38a0e3dfc9c8720c732446ca8f3".hexStringToByteArray()).pubKeyPoint)
                val staticMessages = StaticMessages()

                //auth
                val initiateMessage = handshake.createAuthInitiateV4(myKey)
                println(initiateMessage.toString())
                val initiatePacket = handshake.encryptAuthInitiateV4(initiateMessage)
                output.write(initiatePacket)

                //ack-auth
                val responsePacket = readEIP8Packet(input)
                val initiateMessageResponse = handshake.handleAuthResponseV4(myKey, initiatePacket, responsePacket)
                println(initiateMessageResponse.toString())

                //hello
                val frameCodec = FrameCodec(handshake.secrets)
                val helloMessage = staticMessages.createHelloMessage(myKey.nodeId.toHexString())
                println(helloMessage.toString())
                frameCodec.writeFrame(FrameCodec.Frame(helloMessage.code.toInt(), helloMessage.encoded), output)

                //hello response
                var frames = frameCodec.readFrames(input)
                if (frames == null || frames.isEmpty())
                    return
                var frame = frames[0]
                val payload = frame.stream.readBytes()

                if (frame.type == P2pMessageCodes.HELLO.asByte().toLong()) {
                    val responseHello = HelloMessage(payload)
                    println(responseHello.toString())

                } else {
                    val disconnectMessage = DisconnectMessage(payload)
                    println(disconnectMessage.toString())

//                    channel.getNodeStatistics().nodeDisconnectedRemote(message.getReason())
                }

                //les/2 status
                val les2StatusMessage = StatusMessage(2, 3, "100000".hexStringToByteArray(), genesisBlockHash, BigInteger("0"), genesisBlockHash)
                println(les2StatusMessage.toString())
                frameCodec.writeFrame(FrameCodec.Frame(les2StatusMessage.code + 0x10, les2StatusMessage.encoded), output)

                for (i in 1..1000) {

                    if (input.available() <= 0) {
                        println("not available")
                        Thread.sleep(1000)
                        continue
                    }

                    frames = frameCodec.readFrames(input)
                    if (frames != null && !frames.isEmpty()) {
                        frame = frames[0]

                        val payload = frame.stream.readBytes()

                        when (frame.type) {
                            P2pMessageCodes.HELLO.asByte().toLong() -> {
                                println("P2P - HELLO Message arrived")
                            }
                            P2pMessageCodes.DISCONNECT.asByte().toLong() -> {
                                println("P2P - DISCONNECT Message arrived")
                                input.close()
                                output.close()
                                socket.close()
                            }
                            P2pMessageCodes.PING.asByte().toLong() -> {
                                println("P2P - PING Message arrived")

                                frameCodec.writeFrame(FrameCodec.Frame(P2pMessageCodes.PONG.asByte().toInt(), StaticMessages.PONG_MESSAGE.encoded), output)
                                println("P2P - PONG Message sent")
                            }
                            P2pMessageCodes.PONG.asByte().toLong() -> {
                                println("P2P - PONG Message arrived")
                            }

                            LesMessageCodes.STATUS.asByte().toLong() -> {
                                println("LES - STATUS Message arrived")
                                //les/2 status response
                                val responseStatus = StatusMessage(payload)
                                println(responseStatus.toString())
                                val requestID = Math.abs(Random().nextLong())
                                val getBlockHeaders = GetBlockHeadersMessage(requestID, 0, genesisBlockHash, 10, 0, false)
                                frameCodec.writeFrame(FrameCodec.Frame(LesMessageCodes.GET_BLOCK_HEADERS.asByte().toInt(), getBlockHeaders.encoded), output)
                                println(getBlockHeaders.toString())
                            }

                            LesMessageCodes.BLOCK_HEADERS.asByte().toLong() -> {
                                println("LES - BLOCK_HEADERS Message arrived")

                                val blockHeadersMessage = BlockHeadersMessage(payload)
                                println(blockHeadersMessage.toString())

                            }

                            else -> {
                                println("LES - message = ${frame.type}")
                            }
                        }

                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
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
