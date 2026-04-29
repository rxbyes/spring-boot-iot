package com.ghlzm.iot.message.service.capability;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CapabilityFeedbackTopicMatcher {

    private static final Pattern PATTERN = Pattern.compile("^/(?:iot/)?(?:broadcast|led|flash)/[^/]+/feedback$");

    public boolean matches(String topic) {
        return topic != null && PATTERN.matcher(topic.trim()).matches();
    }
}
