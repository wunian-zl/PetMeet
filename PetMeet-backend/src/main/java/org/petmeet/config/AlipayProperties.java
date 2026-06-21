package org.petmeet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.pay.alipay")
public class AlipayProperties {
    private boolean enabled = false;
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String notifyUrl;
    private String format = "json";
    private String charset = "UTF-8";
    private String signType = "RSA2";
    private String subjectPrefix = "PetMeet订单";

    public boolean isReady() {
        return enabled
                && hasText(appId)
                && hasText(privateKey)
                && hasText(alipayPublicKey)
                && hasText(notifyUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
