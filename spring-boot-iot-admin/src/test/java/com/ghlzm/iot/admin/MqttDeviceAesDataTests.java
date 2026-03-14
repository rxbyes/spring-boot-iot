package com.ghlzm.iot.admin;

import com.ghlxk.cloud.aes.core.AesEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * @author rxbyes
 * @description: TODO
 * @date 2026/3/14 15:43
 */
@SpringBootTest
public class MqttDeviceAesDataTests {

    /**
     *
     *spring-dev.yml 配置文件中定义了各个厂商的密钥，配置内容如下：
     *spring:
     *   cloud:
     *     aes:
     *       merchants:
     *         62000000:
     *           key: Ng93rNxwBoIOM7L9Gm9ATO+lPh2MTmD9o9kuA3u/AaE=
     *           key-derivation: false  # true 派生密钥模式，false：直接秘钥模式
     *           mode: AES/CBC/PKCS5Padding
     *         62000001: # 南方测绘
     *           key: "FWx7ESWwrfx/F7T9bLKfjIOcRdyqnT1ayHqZkB6Q4Ak="
     *           key-derivation: false
     *           mode: AES/CBC/PKCS5Padding
     *         62000002: # 先科
     *           key: "q3FiNTM6CYaVX8RwKI+WVu85K27PKb43Rd6G/cH2+5w="
     *           key-derivation: false
     *           mode: AES/CBC/PKCS5Padding
     *         62000003: # 华源交通（三立）
     *           key: "mTPJ8e6o1WNpk6zaiZTb6dqtUk9+ZaUS742LjU+Ri1A="
     *           #          key: Ng93rNxwBoIOM7L9Gm9ATO+lPh2MTmD9o9kuA3u/AaE=
     *           key-derivation: false
     *           mode: AES/CBC/PKCS5Padding
     */
    Map<String, AesEncryptor> aesEncryptors;

    public MqttDeviceAesDataTests(Map<String, AesEncryptor> aesEncryptors) {
        this.aesEncryptors = aesEncryptors;
    }

    @Test
    public void decode(){

        AesEncryptor aesEncryptor =   aesEncryptors.get("62000001");


       byte[] result =  aesEncryptor
               .decryptByte("8hBxs1xQYrHovuxfNvaZZOasvPiDJB8RgbozUIeVo3hVk70q0/Oaf3GmrhodAd6J0DloTmrcYcq+ieg9I95nBaU+Nr0Yjz/R63it6gePmfBNslGmii28Hgwp5pcj3R5I9Mh7JGsB8sJsxMbUqNShRAeLDtlKU+J1LE4S4rvne9Ab55DinF2u+f+ghUlkXuLUkMJzMx04GxgOo6zX85ADcEd/Et5LHZwLCWqtPh7sNJUwbJO4cCB66L33bCPjVCIynZfzczCb6qhhVZHh0q0uWdohmcCNFYrZkZhwgUDun5HvvVCL2Z7Plgqux6NiaMPW5MlNGBiVZ7sMlK3sUmKWdaerXsTghZ+8HIE3jP6DALCcTA9iphAmyxn7hW3/BAGwfyovt/y4iMMaT2Tf99atnRHqGeq61GELuaTlk0W3AVWIco7Z5XTHnXkYViPIw6/8qJ13EM/1CsWX0pDW6DPNq9wSLnZmXICtzl9VItZTdg4MDYxLoloB8PBdzjQiDgCaPBY/69FfRipmFvj6QvZW7xaudM2rjDfwpGhTg2i7jWA=");

        System.out.printf("解密后的数据：{}",new String(result));

    }
}
