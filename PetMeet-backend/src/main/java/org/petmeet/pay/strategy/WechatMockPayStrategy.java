package org.petmeet.pay.strategy;

import org.petmeet.enums.PayTypeEnum;
import org.petmeet.pay.PayChannelRequest;
import org.petmeet.pay.PayChannelResponse;
import org.petmeet.pay.RefundChannelRequest;
import org.petmeet.pay.RefundChannelResponse;
import org.springframework.stereotype.Component;

@Component
public class WechatMockPayStrategy implements PayStrategy {

    @Override
    public PayTypeEnum getPayType() {
        return PayTypeEnum.WECHAT_MOCK;
    }

    @Override
    public PayChannelResponse createPay(PayChannelRequest request) {
        return PayChannelResponse.builder()
                .success(true)
                .qrCodeUrl("petmeet://pay/wechat-mock/" + request.getPaySn())
                .build();
    }

    @Override
    public RefundChannelResponse refund(RefundChannelRequest request) {
        return RefundChannelResponse.builder()
                .success(true)
                .tradeNo("MOCK_REFUND_" + request.getRefundSn())
                .build();
    }
}
