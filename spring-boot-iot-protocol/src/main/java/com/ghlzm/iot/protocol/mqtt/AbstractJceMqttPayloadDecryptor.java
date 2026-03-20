package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 基于 JCE 的通用对称解密器。
 * 用于承接 DES / 3DES 等常见厂商算法扩展。
 */
abstract class AbstractJceMqttPayloadDecryptor implements MqttPayloadDecryptor {

    private final IotProperties iotProperties;

    protected AbstractJceMqttPayloadDecryptor(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    protected abstract String algorithmCode();

    protected abstract String defaultTransformation();

    @Override
    public boolean supports(String appId) {
        IotProperties.Protocol.Crypto.Merchant merchant = findMerchant(appId);
        return merchant != null && algorithmCode().equalsIgnoreCase(merchant.getAlgorithm());
    }

    @Override
    public byte[] decryptBytes(String appId, String encryptedBody) {
        IotProperties.Protocol.Crypto.Merchant merchant = findMerchant(appId);
        if (merchant == null) {
            throw new BizException("未找到 appId 对应的 " + algorithmCode() + " 解密配置: " + appId);
        }
        if (encryptedBody == null || encryptedBody.isBlank()) {
            throw new BizException(algorithmCode() + " 密文不能为空: " + appId);
        }

        String transformation = merchant.getTransformation();
        if (transformation == null || transformation.isBlank()) {
            transformation = defaultTransformation();
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(merchant.getKey());
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, jceKeyAlgorithm());
            Cipher cipher = Cipher.getInstance(transformation);
            if (transformation.toUpperCase().contains("/ECB/")) {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            } else {
                if (merchant.getIv() == null || merchant.getIv().isBlank()) {
                    throw new BizException(algorithmCode() + " 解密缺少 IV 配置: " + appId);
                }
                byte[] ivBytes = Base64.getDecoder().decode(merchant.getIv());
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
            }
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBody);
            return cipher.doFinal(encryptedBytes);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(algorithmCode() + " 解密失败: " + appId);
        }
    }

    protected String jceKeyAlgorithm() {
        return algorithmCode();
    }

    private IotProperties.Protocol.Crypto.Merchant findMerchant(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        return iotProperties.getProtocol().getCrypto().getMerchants().get(appId);
    }
}
