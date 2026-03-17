package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.message.dispatcher.UpMessageDispatcher;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT 消息接入消费者。
 */
@Component
public class MqttMessageConsumer implements SmartLifecycle, MqttCallbackExtended {

    private final IotProperties iotProperties;
    private final UpMessageDispatcher upMessageDispatcher;
    private final MqttTopicRouter mqttTopicRouter;
    private final DeviceSessionService deviceSessionService;
    private final MqttConnectionListener mqttConnectionListener;

    private volatile boolean running;
    private MqttClient mqttClient;

    public MqttMessageConsumer(IotProperties iotProperties,
                               UpMessageDispatcher upMessageDispatcher,
                               MqttTopicRouter mqttTopicRouter,
                               DeviceSessionService deviceSessionService,
                               MqttConnectionListener mqttConnectionListener) {
        this.iotProperties = iotProperties;
        this.upMessageDispatcher = upMessageDispatcher;
        this.mqttTopicRouter = mqttTopicRouter;
        this.deviceSessionService = deviceSessionService;
        this.mqttConnectionListener = mqttConnectionListener;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        if (!Boolean.TRUE.equals(iotProperties.getMqtt().getEnabled())) {
            mqttConnectionListener.onStartupSkipped("iot.mqtt.enabled=false");
            return;
        }
        if (iotProperties.getMqtt().getBrokerUrl() == null || iotProperties.getMqtt().getBrokerUrl().isBlank()) {
            mqttConnectionListener.onStartupSkipped("iot.mqtt.broker-url 未配置");
            return;
        }

        try {
            mqttClient = new MqttClient(
                    iotProperties.getMqtt().getBrokerUrl(),
                    resolveClientId(),
                    new MemoryPersistence()
            );
            mqttClient.setCallback(this);
            mqttClient.connect(buildConnectOptions());
            subscribeConfiguredTopics();
            running = true;
        } catch (MqttException ex) {
            mqttConnectionListener.onStartupFailed(ex);
        }
    }

    @Override
    public void stop() {
        if (mqttClient == null) {
            running = false;
            return;
        }
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        } catch (MqttException ex) {
            mqttConnectionListener.onShutdownFailed(ex);
        } finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        running = true;
        mqttConnectionListener.onConnectComplete(reconnect, serverURI);
        if (reconnect) {
            subscribeConfiguredTopics();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        running = false;
        mqttConnectionListener.onConnectionLost(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        RawDeviceMessage rawDeviceMessage = null;
        String traceId = TraceContextHolder.bindTraceId(null);
        try {
            mqttConnectionListener.onMessageReceived(topic, message == null || message.getPayload() == null
                    ? 0
                    : message.getPayload().length);
            rawDeviceMessage = mqttTopicRouter.toRawMessage(topic, message);
            rawDeviceMessage.setTraceId(traceId);

            DeviceUpMessage upMessage = upMessageDispatcher.dispatch(rawDeviceMessage);
            String resolvedDeviceCode = hasText(upMessage.getDeviceCode())
                    ? upMessage.getDeviceCode()
                    : rawDeviceMessage.getDeviceCode();
            mqttConnectionListener.onMessageDispatched(topic, resolvedDeviceCode, upMessage.getMessageType(), upMessage.getTraceId());

            String resolvedClientId = hasText(rawDeviceMessage.getClientId())
                    ? rawDeviceMessage.getClientId()
                    : resolvedDeviceCode;
            deviceSessionService.online(resolvedDeviceCode, resolvedClientId);
            deviceSessionService.refreshLastSeen(resolvedDeviceCode, resolvedClientId, topic);
            mqttConnectionListener.onDeviceSessionRefreshed(resolvedDeviceCode, resolvedClientId, topic);
        } catch (Exception ex) {
            mqttConnectionListener.onMessageDispatchFailed(topic, rawDeviceMessage, ex);
        } finally {
            TraceContextHolder.clear();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 当前只处理上行接入。
    }

    /**
     * 复用同一 MQTT 客户端执行下行发布。
     */
    public void publish(String topic, byte[] payload, int qos, boolean retained) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new BizException("MQTT 客户端未连接，无法发布消息");
        }
        try {
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException ex) {
            throw new BizException("MQTT 消息发布失败");
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    private void subscribeConfiguredTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        List<String> topics = mqttTopicRouter.resolveSubscribeTopics();
        try {
            for (String topic : topics) {
                mqttClient.subscribe(topic, iotProperties.getMqtt().getQos());
            }
            mqttConnectionListener.onSubscribe(topics);
        } catch (MqttException ex) {
            mqttConnectionListener.onSubscribeFailed(topics, ex);
        }
    }

    private String resolveClientId() {
        if (iotProperties.getMqtt().getClientId() != null && !iotProperties.getMqtt().getClientId().isBlank()) {
            return iotProperties.getMqtt().getClientId();
        }
        return "spring-boot-iot-mqtt-consumer";
    }

    private MqttConnectOptions buildConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(Boolean.TRUE.equals(iotProperties.getMqtt().getCleanSession()));
        options.setConnectionTimeout(iotProperties.getMqtt().getConnectionTimeout());
        options.setKeepAliveInterval(iotProperties.getMqtt().getKeepAliveInterval());
        if (iotProperties.getMqtt().getUsername() != null) {
            options.setUserName(iotProperties.getMqtt().getUsername());
        }
        if (iotProperties.getMqtt().getPassword() != null) {
            options.setPassword(iotProperties.getMqtt().getPassword().toCharArray());
        }
        return options;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
