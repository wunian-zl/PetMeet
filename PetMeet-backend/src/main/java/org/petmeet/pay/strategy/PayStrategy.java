package org.petmeet.pay.strategy;

import org.petmeet.enums.PayTypeEnum;
import org.petmeet.pay.PayCallbackResult;
import org.petmeet.pay.PayChannelRequest;
import org.petmeet.pay.PayChannelResponse;
import org.petmeet.pay.PayQueryResult;
import org.petmeet.pay.RefundChannelRequest;
import org.petmeet.pay.RefundChannelResponse;

import java.util.Map;

public interface PayStrategy {
    PayTypeEnum getPayType();

    PayChannelResponse createPay(PayChannelRequest request);

    default PayQueryResult queryPay(PayChannelRequest request) {
        return PayQueryResult.builder()
                .success(false)
                .errorMsg("当前支付通道不支持主动查单")
                .build();
    }

    RefundChannelResponse refund(RefundChannelRequest request);

    default PayCallbackResult verifyCallback(Map<String, String> params) {
        return PayCallbackResult.builder()
                .verified(false)
                .errorMsg("当前支付通道不支持异步回调")
                .build();
    }
}
