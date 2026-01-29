package com.ecommerce.apigateway;
import java.security.*;
import java.util.Base64;
import java.io.FileOutputStream;

public class KeyGen {
    public static void main(String[] args) throws Exception {
        // 1. Generate a NEW, SECURE 2048-bit pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        // 2. Write Private Key (PKCS#8 format for Java)
        try (FileOutputStream fos = new FileOutputStream("private.pem")) {
            fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
            fos.write(Base64.getMimeEncoder().encode(pair.getPrivate().getEncoded()));
            fos.write("\n-----END PRIVATE KEY-----".getBytes());
        }

        // 3. Write Public Key (X.509 format for Gateway)
        try (FileOutputStream fos = new FileOutputStream("public.pem")) {
            fos.write("-----BEGIN PUBLIC KEY-----\n".getBytes());
            fos.write(Base64.getMimeEncoder().encode(pair.getPublic().getEncoded()));
            fos.write("\n-----END PUBLIC KEY-----".getBytes());
        }

        System.out.println("✅ NEW secure keys generated! Do not share the private.pem file.");
    }
}