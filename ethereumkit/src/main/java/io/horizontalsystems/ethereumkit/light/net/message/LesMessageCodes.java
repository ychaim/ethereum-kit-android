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


/**
 * A list of commands for the Ethereum network protocol.
 * <br>
 * The codes for these commands are the first byte in every packet.
 *
 * @see <a href="https://github.com/ethereum/wiki/wiki/Ethereum-Wire-Protocol">
 * https://github.com/ethereum/wiki/wiki/Ethereum-Wire-Protocol</a>
 */
public enum LesMessageCodes {

    /* Ethereum protocol */

    /**
     * {@code [0x00, [PROTOCOL_VERSION, NETWORK_ID, TD, BEST_HASH, GENESIS_HASH] } <br>
     * <p>
     * Inform a peer of it's current ethereum state. This message should be
     * send after the initial handshake and prior to any ethereum related messages.
     */
    STATUS(0x00 + 0x10),

    /**
     * {@code [+0x03: P, block: { P , B_32 }, maxHeaders: P, skip: P, reverse: P in { 0 , 1 } ] } <br>
     * <p>
     * Replaces GetBlockHashes since PV 62. <br>
     * <p>
     * Require peer to return a BlockHeaders message.
     * Reply must contain a number of block headers,
     * of rising number when reverse is 0, falling when 1, skip blocks apart,
     * beginning at block block (denoted by either number or hash) in the canonical chain,
     * and with at most maxHeaders items.
     */
    GET_BLOCK_HEADERS(0x02 + 0x10),

    /**
     * {@code [+0x04, blockHeader_0, blockHeader_1, ...] } <br>
     * <p>
     * Replaces BLOCK_HASHES since PV 62. <br>
     * <p>
     * Reply to GetBlockHeaders.
     * The items in the list (following the message ID) are
     * block headers in the format described in the main Ethereum specification,
     * previously asked for in a GetBlockHeaders message.
     * This may validly contain no block headers
     * if no block headers were able to be returned for the GetBlockHeaders query.
     */
    BLOCK_HEADERS(0x03 + 0x10),

    /**
     * {@code [+0x05, hash_0: B_32, hash_1: B_32, ...] } <br>
     * <p>
     * Replaces GetBlocks since PV 62. <br>
     * <p>
     * Require peer to return a BlockBodies message.
     * Specify the set of blocks that we're interested in with the hashes.
     */
    GET_BLOCK_BODIES(0x04 + 0x10),

    /**
     * {@code [+0x06, [transactions_0, uncles_0] , ...] } <br>
     * <p>
     * Replaces Blocks since PV 62. <br>
     * <p>
     * Reply to GetBlockBodies.
     * The items in the list (following the message ID) are some of the blocks, minus the header,
     * in the format described in the main Ethereum specification, previously asked for in a GetBlockBodies message.
     * This may validly contain no block headers
     * if no block headers were able to be returned for the GetBlockHeaders query.
     */
    BLOCK_BODIES(0x05 + 0x10),

    /**
     * {@code [+0x0f, hash_0: B_32, hash_1: B_32, ...] } <br>
     * <p>
     * Require peer to return a Receipts message. Hint that useful values in it
     * are those which correspond to blocks of the given hashes.
     */
    GET_RECEIPTS(0x06 + 0x10),

    /**
     * {@code [+0x10, [receipt_0, receipt_1], ...] } <br>
     * <p>
     * Provide a set of receipts which correspond to previously asked in GetReceipts.
     */
    RECEIPTS(0x7 + 0x10),

    GET_PROOFS(0x8 + 0x10),
    PROOFS(0x9 + 0x10),
    GET_PROOFS_V2(0x0f + 0x10),
    PROOFS_V2(0x10 + 0x10);

    private int cmd;

    private static final LesMessageCodes[] messageCodes;

    static {

        messageCodes = new LesMessageCodes[]{
                STATUS
        };
//
//        versionToValuesMap.put(V63, new EthMessageCodes[]{
//                STATUS,
//                NEW_BLOCK_HASHES,
//                TRANSACTIONS,
//                GET_BLOCK_HEADERS,
//                BLOCK_HEADERS,
//                GET_BLOCK_BODIES,
//                BLOCK_BODIES,
//                NEW_BLOCK,
//                GET_NODE_DATA,
//                NODE_DATA,
//                GET_RECEIPTS,
//                RECEIPTS
//        });
//
//        for (EthVersion v : EthVersion.values()) {
//            Map<Integer, EthMessageCodes> map = new HashMap<>();
//            intToTypeMap.put(v, map);
//            for (EthMessageCodes code : values(v)) {
//                map.put(code.cmd, code);
//            }
//        }
    }

    private LesMessageCodes(int cmd) {
        this.cmd = cmd;
    }

    //    public static EthMessageCodes[] values(EthVersion v) {
//        return versionToValuesMap.get(v);
//    }
//
//    public static int maxCode(EthVersion v) {
//
//        int max = 0;
//        for (EthMessageCodes cd : versionToValuesMap.get(v))
//            if (max < cd.asByte())
//                max = cd.asByte();
//
//        return max;
//    }
//
    public static LesMessageCodes fromByte(byte i) {
        return STATUS;
    }
//
//    public static boolean inRange(byte code, EthVersion v) {
//        EthMessageCodes[] codes = values(v);
//        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
//    }

    public byte asByte() {
        return (byte) (cmd);
    }
}