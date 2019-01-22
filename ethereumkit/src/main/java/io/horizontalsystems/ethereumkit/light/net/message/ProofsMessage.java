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
import java.util.Arrays;

import io.horizontalsystems.ethereumkit.light.util.ByteUtil;
import io.horizontalsystems.ethereumkit.light.util.RLP;
import io.horizontalsystems.ethereumkit.light.util.RLPElement;
import io.horizontalsystems.ethereumkit.light.util.RLPList;


public class ProofsMessage extends LesMessage {

    private long requestID;

    private long BV;

    private RLPList rlpList;

    private long nonce;
    private BigInteger balance;
    private String storageRoot;
    private String codeHash;

    public ProofsMessage(byte[] encoded) {
        super(encoded);
    }

    @Override
    public byte[] getEncoded() {
        return new byte[0];
    }

    private synchronized void parse() {
        if (parsed) return;


        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        this.requestID = ByteUtil.byteArrayToLong(paramsList.get(0).getRLPData());
        this.BV = ByteUtil.byteArrayToLong(paramsList.get(1).getRLPData());

        rlpList = (RLPList) paramsList.get(2);

        RLPList.recursivePrint(rlpList);

        RLPList lastNodeValue = (RLPList) rlpList.get(rlpList.size() - 1);
        TrieNode lastNode = new TrieNode(lastNodeValue);

        if (lastNode.nodeType != TrieNode.NodeType.LEAF) {
            System.out.println("Last Node is not LEAF Node");
            return;
        }

        RLPElement valueRLP = lastNodeValue.get(1);
        RLPList value = (RLPList) RLP.decode2(valueRLP.getRLPData()).get(0);

        nonce = ByteUtil.byteArrayToLong(value.get(0).getRLPData());
        balance = ByteUtil.bytesToBigInteger(value.get(1).getRLPData());
        storageRoot = ByteUtil.toHexString(value.get(2).getRLPData());
        codeHash = ByteUtil.toHexString(value.get(3).getRLPData());

        parsed = true;
    }

    public boolean validProof(byte[] stateRoot, byte[] addressHash) {
        RLPList lastNodeValue = (RLPList) rlpList.get(rlpList.size() - 1);
        TrieNode lastNode = new TrieNode(lastNodeValue);

        if (lastNode.nodeType != TrieNode.NodeType.LEAF) {
            System.out.println("Last Node is not LEAF Node");
            return false;
        }

        String path = lastNode.getPath(null);
        byte[] lastNodeKey = lastNode.hash;

        for (int i = rlpList.size() - 2; i>=0; i--) {
            lastNodeValue = (RLPList) rlpList.get(i);
            lastNode = new TrieNode(lastNodeValue);
            String partialPath = lastNode.getPath(lastNodeKey);

            if (partialPath != null) {
                path = partialPath + path;
            } else {
                System.out.println("Nodes are not interconnected");
                return false;
            }

            lastNodeKey = lastNode.hash;
        }

        System.out.println("Path: " + path);
        System.out.println("lastNodeKey: " + ByteUtil.toHexString(lastNodeKey));

        if (!ByteUtil.toHexString(addressHash).equals(path)) {
            System.out.println("Path does not match addressHash");
            return false;
        }

        if (!Arrays.equals(stateRoot, lastNodeKey)) {
            System.out.println("rootHash does not match stateRoot");
            return false;
        }

        return true;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public LesMessageCodes getCommand() {
        return LesMessageCodes.GET_PROOFS_V2;
    }

    @Override
    public String toString() {
        parse();

        StringBuilder payload = new StringBuilder();
        payload.append("requestID=").append(requestID).append(", BV=").append(BV);

        payload.append(" ");
        payload.append("\nnonce = ").append(nonce);
        payload.append("\nbalance = ").append(balance);
        payload.append("\nstorageRoot = ").append(storageRoot);
        payload.append("\ncodeHash = ").append(codeHash);

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
