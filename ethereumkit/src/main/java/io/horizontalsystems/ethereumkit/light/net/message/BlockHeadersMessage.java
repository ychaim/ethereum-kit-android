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


import java.util.ArrayList;
import java.util.List;

import io.horizontalsystems.ethereumkit.light.util.ByteUtil;
import io.horizontalsystems.ethereumkit.light.util.RLP;
import io.horizontalsystems.ethereumkit.light.util.RLPList;

import static io.horizontalsystems.ethereumkit.light.util.ByteUtil.toHexString;


/**
 * Wrapper around an Ethereum BlockHeaders message on the network
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockHeadersMessage extends LesMessage {

    /**
     * List of block headers from the peer
     */

    private long requestID;

    private long BV;

    private List<BlockHeader> blockHeaders;

    public BlockHeadersMessage(byte[] encoded) {
        super(encoded);
    }

    public BlockHeadersMessage(List<BlockHeader> headers) {
        this.blockHeaders = headers;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;

        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        this.requestID = ByteUtil.byteArrayToLong(paramsList.get(0).getRLPData());
        this.BV = ByteUtil.byteArrayToLong(paramsList.get(1).getRLPData());

        RLPList payloadList = (RLPList) paramsList.get(2);
        blockHeaders = new ArrayList<>();
        for (int i = 0; i < payloadList.size(); ++i) {
            RLPList rlpData = ((RLPList) payloadList.get(i));
            blockHeaders.add(new BlockHeader(rlpData));
        }
        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (BlockHeader blockHeader : blockHeaders)
            encodedElements.add(blockHeader.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public List<BlockHeader> getBlockHeaders() {
        parse();
        return blockHeaders;
    }

    @Override
    public LesMessageCodes getCommand() {
        return LesMessageCodes.BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        parse();

        StringBuilder payload = new StringBuilder();
        payload.append("requestID=").append(requestID).append(", BV=").append(BV);
        payload.append(" count( ").append(blockHeaders.size()).append(" )");

//        if (logger.isTraceEnabled()) {
            payload.append(" ");
            for (BlockHeader header : blockHeaders) {
                payload.append(toHexString(header.getHash())).append(" | ");
            }
            if (!blockHeaders.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }


        return "[" + getCommand().name() + " " + payload + "]";
    }
}
