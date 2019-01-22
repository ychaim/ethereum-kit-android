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


import java.math.BigInteger;

import io.horizontalsystems.ethereumkit.light.util.ByteUtil;
import io.horizontalsystems.ethereumkit.light.util.RLP;

import static io.horizontalsystems.ethereumkit.light.util.ByteUtil.EMPTY_BYTE_ARRAY;

public class GetProofsMessage extends LesMessage {

    private static final int DEFAULT_SIZE_BYTES = 32;
    private byte[] key;
    private byte[] blockHash;
    private long requestID;

    public GetProofsMessage(long requestID, byte[] blockHash, byte[] key) {
        this.requestID = requestID;
        this.blockHash = blockHash;
        this.key = key;

        parsed = true;
        encode();
    }

    private void encode() {
        byte[] reqID = RLP.encodeBigInteger(BigInteger.valueOf(this.requestID));

        byte[] hash = RLP.encodeElement(this.blockHash);
        byte[] key = RLP.encodeElement(this.key);

        byte[] proofRequest = RLP.encodeList(hash, RLP.encodeElement(EMPTY_BYTE_ARRAY), key, RLP.encodeInt(0));
        byte[] proofRequests = RLP.encodeList(proofRequest);

        this.encoded = RLP.encodeList(reqID, proofRequests);
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public byte[] getKey() {
        return key;
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<BlockHeadersMessage> getAnswerMessage() {
        return BlockHeadersMessage.class;
    }

    @Override
    public LesMessageCodes getCommand() {
        return LesMessageCodes.GET_PROOFS_V2;
    }

    @Override
    public String toString() {
        return "[" + this.getCommand().name() +
                " requestId=" + this.requestID +
                " blockHash=" + ByteUtil.toHexString(blockHash) +
                " key=" + ByteUtil.toHexString(key) +
                "]";
    }
}
