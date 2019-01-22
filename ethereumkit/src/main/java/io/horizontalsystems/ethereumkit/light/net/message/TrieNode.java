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
import java.util.Arrays;
import java.util.List;

import io.horizontalsystems.ethereumkit.light.crypto.HashUtil;
import io.horizontalsystems.ethereumkit.light.util.ByteUtil;
import io.horizontalsystems.ethereumkit.light.util.RLPElement;
import io.horizontalsystems.ethereumkit.light.util.RLPList;


public class TrieNode {

    enum NodeType {
        NULL,
        BRANCH,
        EXTENSION,
        LEAF
    }

    private static char[] alphabet = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public NodeType nodeType;

    private List<byte[]> elements;
    private String encodedPath;
    public byte[] hash;


    public TrieNode(RLPList elements) {
        this.elements = new ArrayList<>();
        for (RLPElement element : elements) {
            this.elements.add(element.getRLPData());
        }

        this.hash = HashUtil.sha3(elements.getRLPData());

        if (this.elements.size() == 17) {
            this.nodeType = NodeType.BRANCH;
        } else {
            byte[] first = this.elements.get(0);
            byte nibble = (byte) (first[0] >> 4);

            switch (nibble) {
                case 0:
                    this.nodeType = NodeType.EXTENSION;
                    encodedPath = ByteUtil.toHexString(Arrays.copyOfRange(first, 1, first.length));
                    break;

                case 1:
                    this.nodeType = NodeType.EXTENSION;

                    encodedPath = ByteUtil.toHexString(Arrays.copyOfRange(first, 1, first.length));
                    byte firstByte = (byte) ((first[0] << 4) >> 4);
                    String firstByteString = ByteUtil.toHexString(new byte[]{firstByte});
                    encodedPath = firstByteString.substring(1) + encodedPath;
                    break;

                case 2:
                    this.nodeType = NodeType.LEAF;
                    encodedPath = ByteUtil.toHexString(Arrays.copyOfRange(first, 1, first.length));
                    break;

                case 3:
                    this.nodeType = NodeType.LEAF;

                    encodedPath = ByteUtil.toHexString(Arrays.copyOfRange(first, 1, first.length));
                    firstByte = (byte) ((first[0] << 4) >> 4);
                    firstByteString = ByteUtil.toHexString(new byte[]{firstByte});
                    encodedPath = firstByteString.substring(1) + encodedPath;
                    break;
            }
        }
    }

    public String getPath(byte[] element) {
        if (element == null && nodeType == NodeType.LEAF) {
            return encodedPath;
        }

        for (int i = 0; i < elements.size(); i++) {
            if (Arrays.equals(elements.get(i), element)) {
                if (nodeType == NodeType.BRANCH) {
                    return String.valueOf(alphabet[i]);
                } else if (nodeType == NodeType.EXTENSION) {
                    return encodedPath;
                }
            }
        }

        return null;
    }

}
