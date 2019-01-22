package io.horizontalsystems.ethereumkit.light.net;

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.crypto.HashUtil
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
            val address = "f757461bdc25ee2b047d545a50768e52d530b750".hexStringToByteArray()
            val address2 = "37531e574427BDE92d9B3a3c2291D9A004827435".hexStringToByteArray()
            val address3 = "1b763c4b9632d6876D83B2270fF4d01b792DE479".hexStringToByteArray()
            val address4 = "401CB37eFa5d82dC51FB599e6A4B1D2b3aaeb2B2".hexStringToByteArray()
            val lastBlockhash = "bec7824a4ee5f616e050d885c3b943c7933af24cdb5cacb9f871063741d1a116".hexStringToByteArray()
            val lastBlockHeaderStateRootHash = "2f9260472bde15e4c9cf55f9a158ae1d5852bb8de51fdf515ce8077b8a6a7213".hexStringToByteArray()

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
                println(">>> " + helloMessage.toString())
                frameCodec.writeFrame(FrameCodec.Frame(helloMessage.code.toInt(), helloMessage.encoded), output)

                //hello response
                var frames = frameCodec.readFrames(input)
                if (frames == null || frames.isEmpty())
                    return
                var frame = frames[0]
                val payload = frame.stream.readBytes()

                if (frame.type == P2pMessageCodes.HELLO.asByte().toLong()) {
                    val responseHello = HelloMessage(payload)
                    println("<<< " + responseHello.toString())

                } else {
                    val disconnectMessage = DisconnectMessage(payload)
                    println(disconnectMessage.toString())

//                    channel.getNodeStatistics().nodeDisconnectedRemote(message.getReason())
                }

                //les/2 status
                val les2StatusMessage = StatusMessage(2, 3, "100000".hexStringToByteArray(), genesisBlockHash, BigInteger("0"), genesisBlockHash)
                println(">>> " + les2StatusMessage.toString())
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
                                println("<<< " + responseStatus.toString())
                                val requestID = Math.abs(Random().nextLong())
                                val getBlockHeaders = GetBlockHeadersMessage(requestID, 0, genesisBlockHash, 10, 0, false)
                                frameCodec.writeFrame(FrameCodec.Frame(LesMessageCodes.GET_BLOCK_HEADERS.asByte().toInt(), getBlockHeaders.encoded), output)
                                println(">>> " + getBlockHeaders.toString())
                            }

                            LesMessageCodes.BLOCK_HEADERS.asByte().toLong() -> {
                                println("LES - BLOCK_HEADERS Message arrived")

                                val blockHeadersMessage = BlockHeadersMessage(payload)
                                println(blockHeadersMessage.toString())

                                val requestId = Math.abs(Random().nextLong())
                                val getProofs = GetProofsMessage(requestId, lastBlockhash, HashUtil.sha3(address))
                                frameCodec.writeFrame(FrameCodec.Frame(LesMessageCodes.GET_PROOFS_V2.asByte().toInt(), getProofs.encoded), output)
                                println(">>> " + getProofs.toString())
                            }

                            LesMessageCodes.PROOFS_V2.asByte().toLong() -> {
                                println("LES - PROOFS Message arrived")

                                val proofsMessage = ProofsMessage(payload)
                                println("<<< " + proofsMessage.toString())

                                if (proofsMessage.validProof(lastBlockHeaderStateRootHash, HashUtil.sha3(address))) {
                                    println("Proof is Valid")
                                } else {
                                    println("Proof is inValid!!!")
                                }
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
