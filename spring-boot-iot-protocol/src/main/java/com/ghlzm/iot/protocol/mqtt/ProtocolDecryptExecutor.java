package com.ghlzm.iot.protocol.mqtt;

public interface ProtocolDecryptExecutor {

    boolean supports(String algorithm);

    byte[] decryptBytes(ProtocolDecryptProfile profile, String encryptedBody);
}
