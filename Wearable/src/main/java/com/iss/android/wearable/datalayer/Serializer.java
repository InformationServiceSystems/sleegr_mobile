/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iss.android.wearable.datalayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * @author Iaroslav
 *         WARNING: (DE)SERIALIZATION IS PERFORMED IN MEMORY FOR BETTER PERFORMANCE
 */
public class Serializer {

    // Serialises an object to a byte array
    public static byte[] SerializeToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

}
