/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package io.horizontalsystems.ethereumkit.light.net.message;


import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.horizontalsystems.ethereumkit.light.net.Capability;
import io.horizontalsystems.ethereumkit.light.net.p2p.DisconnectMessage;
import io.horizontalsystems.ethereumkit.light.net.p2p.GetPeersMessage;
import io.horizontalsystems.ethereumkit.light.net.p2p.HelloMessage;
import io.horizontalsystems.ethereumkit.light.net.p2p.PingMessage;
import io.horizontalsystems.ethereumkit.light.net.p2p.PongMessage;

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.
 *
 * @author Roman Mandeleil
 * @since 13.04.14
 */
public class StaticMessages {

//    SystemProperties config;

//    ConfigCapabilities configCapabilities;

    public final static PingMessage PING_MESSAGE = new PingMessage();
    public final static PongMessage PONG_MESSAGE = new PongMessage();
    public final static GetPeersMessage GET_PEERS_MESSAGE = new GetPeersMessage();
    public final static DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage(ReasonCode.REQUESTED);

    public static final byte[] SYNC_TOKEN = Hex.decode("22400891");

    public HelloMessage createHelloMessage(String peerId) {
//        return createHelloMessage(peerId, config.listenPort());
        return createHelloMessage(peerId, 30303);
    }

    public HelloMessage createHelloMessage(String peerId, int listenPort) {

        String helloAnnouncement = "EthereumKit";
        byte p2pVersion = 4;//(byte) config.defaultP2PVersion();
        List<Capability> capabilities = new ArrayList<Capability>();//configCapabilities.getConfigCapabilities();
        capabilities.add(new Capability("p2p", (byte) 4));
        capabilities.add(new Capability("les", (byte) 2));

        return new HelloMessage(p2pVersion, helloAnnouncement,
                capabilities, listenPort, peerId);
    }

}
