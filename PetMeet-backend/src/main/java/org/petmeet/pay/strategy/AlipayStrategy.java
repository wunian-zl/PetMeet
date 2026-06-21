package org.petmeet.pay.strategy;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.common.AppException;
import org.petmeet.config.AlipayProperties;
import org.petmeet.enums.PayTypeEnum;
import org.petmeet.pay.PayCallbackResult;
import org.petmeet.pay.PayChannelRequest;
import org.petmeet.pay.PayChannelResponse;
import org.petmeet.pay.PayQueryResult;
import org.petmeet.pay.RefundChannelRequest;
import org.petmeet.pay.RefundChannelResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayStrategy implements PayStrategy {

    private final AlipayProperties properties;

    @Override
    public PayTypeEnum getPayType() {
        return PayTypeEnum.ALIPAY;
    }

    @Override
    public PayChannelResponse createPay(PayChannelRequest request) {
        ensureReady();
        AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
        alipayRequest.setNotifyUrl(properties.getNotifyUrl());

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", request.getPaySn());
        biz.put("total_amount", request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        biz.put("subject", buildSubject(request));
        biz.put("timeout_express", resolveTimeoutExpress(request));
        biz.put("qr_code_timeout_express", resolveTimeoutExpress(request));
        alipayRequest.setBizContent(JSON.toJSONString(biz));

        try {
            AlipayTradePrecreateResponse response = client().execute(alipayRequest);
            if (!response.isSuccess()) {
                String msg = response.getSubMsg() == null ? response.getMsg() : response.getSubMsg();
                return PayChannelResponse.builder().success(false).errorMsg(msg).build();
            }
            return PayChannelResponse.builder()
                    .success(true)
                    .qrCodeUrl(response.getQrCode())
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝预创建支付失败:{}", e.getMessage());
            return PayChannelResponse.builder().success(false).errorMsg(e.getMessage()).build();
        }
    }

    @Override
    public PayQueryResult queryPay(PayChannelRequest request) {
        ensureReady();
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", request.getPaySn());
        alipayRequest.setBizContent(JSON.toJSONString(biz));

        try {
            AlipayTradeQueryResponse response = client().execute(alipayRequest);
            if (!response.isSuccess()) {
                String msg = response.getSubMsg() == null ? response.getMsg() : response.getSubMsg();
                return PayQueryResult.builder()
                        .success(false)
                        .rawContent(response.getBody())
                        .errorMsg(msg)
                        .build();
            }

            String tradeStatus = response.getTradeStatus();
            boolean paid = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
            boolean closed = "TRADE_CLOSED".equals(tradeStatus);
            return PayQueryResult.builder()
                    .success(true)
                    .paid(paid)
                    .closed(closed)
                    .tradeNo(response.getTradeNo())
                    .tradeStatus(tradeStatus)
                    .totalAmount(response.getTotalAmount() == null ? null : new java.math.BigDecimal(response.getTotalAmount()))
                    .rawContent(response.getBody())
                    .build();
        } catch (Exception e) {
            log.warn("支付宝查单失败:{}", e.getMessage());
            return PayQueryResult.builder().success(false).errorMsg(e.getMessage()).build();
        }
    }

    @Override
    public RefundChannelResponse refund(RefundChannelRequest request) {
        ensureReady();
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", request.getPaySn());
        if (request.getTradeNo() != null && !request.getTradeNo().isBlank()) {
            biz.put("trade_no", request.getTradeNo());
        }
        biz.put("refund_amount", request.getRefundAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        biz.put("refund_reason", request.getReason());
        biz.put("out_request_no", request.getRefundSn());
        alipayRequest.setBizContent(JSON.toJSONString(biz));
        try {
            AlipayTradeRefundResponse response = client().execute(alipayRequest);
            if (!response.isSuccess()) {
                String msg = response.getSubMsg() == null ? response.getMsg() : response.getSubMsg();
                return RefundChannelResponse.builder().success(false).errorMsg(msg).build();
            }
            return RefundChannelResponse.builder()
                    .success(true)
                    .tradeNo(response.getTradeNo())
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝退款失败:{}", e.getMessage());
            return RefundChannelResponse.builder().success(false).errorMsg(e.getMessage()).build();
        }
    }

    @Override
    public PayCallbackResult verifyCallback(Map<String, String> params) {
        ensureReady();
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType()
            );
            if (!verified) {
                return PayCallbackResult.builder().verified(false).errorMsg("支付宝回调验签失败").build();
            }
            String amount = params.get("total_amount");
            return PayCallbackResult.builder()
                    .verified(true)
                    .appId(params.get("app_id"))
                    .paySn(params.get("out_trade_no"))
                    .tradeNo(params.get("trade_no"))
                    .tradeStatus(params.get("trade_status"))
                    .totalAmount(amount == null ? null : new java.math.BigDecimal(amount))
                    .build();
        } catch (Exception e) {
            log.warn("支付宝回调验签异常:{}", e.getMessage());
            return PayCallbackResult.builder().verified(false).errorMsg(e.getMessage()).build();
        }
    }

    private String buildSubject(PayChannelRequest request) {
        return properties.getSubjectPrefix() + request.getOrderSn();
    }

    private String resolveTimeoutExpress(PayChannelRequest request) {
        if (request.getExpireTime() == null) {
            return "30m";
        }
        long seconds = Duration.between(LocalDateTime.now(), request.getExpireTime()).getSeconds();
        long minutes = Math.max(1L, (seconds + 59L) / 60L);
        return minutes + "m";
    }

    private AlipayClient client() {
        return new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getPrivateKey(),
                properties.getFormat(),
                properties.getCharset(),
                properties.getAlipayPublicKey(),
                properties.getSignType()
        );
    }

    private void ensureReady() {
        if (!properties.isReady()) {
            throw AppException.badRequest("支付宝沙箱未配置完整");
        }
    }
}
