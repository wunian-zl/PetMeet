package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.common.AppException;
import org.petmeet.dto.PayCreateDTO;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.OmsPayLog;
import org.petmeet.entity.OmsRefundLog;
import org.petmeet.entity.PmsProduct;
import org.petmeet.enums.PayModeEnum;
import org.petmeet.enums.PayStatusEnum;
import org.petmeet.enums.PayTypeEnum;
import org.petmeet.enums.RefundStatusEnum;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsPayLogMapper;
import org.petmeet.mapper.OmsRefundLogMapper;
import org.petmeet.pay.PayCallbackResult;
import org.petmeet.pay.PayChannelRequest;
import org.petmeet.pay.PayChannelResponse;
import org.petmeet.pay.PayQueryResult;
import org.petmeet.pay.RefundChannelRequest;
import org.petmeet.pay.RefundChannelResponse;
import org.petmeet.pay.strategy.PayStrategy;
import org.petmeet.pay.strategy.PayStrategyFactory;
import org.petmeet.service.PayService;
import org.petmeet.service.PmsProductService;
import org.petmeet.vo.PayResponseVO;
import org.petmeet.vo.PayStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private static final DateTimeFormatter SN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OmsOrderMapper orderMapper;
    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final OmsPayLogMapper payLogMapper;
    private final OmsRefundLogMapper refundLogMapper;
    private final PmsProductService productService;
    private final PayStrategyFactory strategyFactory;

    @Value("${app.order.pay-timeout-minutes:30}")
    private Integer payTimeoutMinutes;

    @Value("${app.pay.alipay.app-id:}")
    private String alipayAppId;

    @Value("${app.pay.alipay.gateway-url:}")
    private String alipayGatewayUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponseVO createPay(PayCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        PayTypeEnum payType = PayTypeEnum.fromValue(dto == null ? null : dto.getPayType());
        PayModeEnum payMode = PayModeEnum.fromValue(dto == null ? null : dto.getPayMode());
        boolean forceRefresh = dto != null && Boolean.TRUE.equals(dto.getForceRefresh());
        OmsOrder order = requirePayableOrder(dto, userId);

        OmsPayLog existing = findLatestPendingLog(order.getId(), payType);
        LocalDateTime now = LocalDateTime.now();
        if (!forceRefresh && existing != null && existing.getExpireTime() != null && existing.getExpireTime().isAfter(now)) {
            return toPayResponse(existing);
        }
        if (existing != null) {
            closePayLog(existing.getId(), forceRefresh ? "用户重新生成支付二维码" : "支付流水已过期");
        }

        LocalDateTime expireTime = now.plusMinutes(resolveTimeoutMinutes());
        OmsPayLog payLog = new OmsPayLog();
        payLog.setPaySn(nextSn("PAY"));
        payLog.setOrderId(order.getId());
        payLog.setOrderSn(order.getOrderSn());
        payLog.setUserId(order.getUserId());
        payLog.setPayType(payType.getCode());
        payLog.setPayMode(payMode.getCode());
        payLog.setPayAmount(order.getTotalAmount());
        payLog.setPayStatus(PayStatusEnum.PENDING.getCode());
        payLog.setExpireTime(expireTime);
        payLog.setCreateTime(now);
        payLogMapper.insert(payLog);

        PayStrategy strategy = strategyFactory.getStrategy(payType);
        try {
            PayChannelResponse channelResponse = strategy.createPay(PayChannelRequest.builder()
                    .paySn(payLog.getPaySn())
                    .orderSn(order.getOrderSn())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .payType(payType)
                    .payMode(payMode)
                    .amount(order.getTotalAmount())
                    .subject("PetMeet订单" + order.getOrderSn())
                    .expireTime(expireTime)
                    .build());
            if (!channelResponse.isSuccess()) {
                failPayLog(payLog.getId(), channelResponse.getErrorMsg());
                throw AppException.badRequest(StrUtil.blankToDefault(channelResponse.getErrorMsg(), "创建支付失败"));
            }
            OmsPayLog update = new OmsPayLog();
            update.setId(payLog.getId());
            update.setQrCodeUrl(channelResponse.getQrCodeUrl());
            update.setTradeNo(channelResponse.getTradeNo());
            update.setUpdateTime(LocalDateTime.now());
            payLogMapper.updateById(update);
            payLog.setQrCodeUrl(channelResponse.getQrCodeUrl());
            payLog.setTradeNo(channelResponse.getTradeNo());
            return toPayResponse(payLog);
        } catch (AppException e) {
            failPayLog(payLog.getId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            failPayLog(payLog.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayStatusVO queryPayStatus(String paySn, boolean syncChannel) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsPayLog payLog = requireMyPayLog(paySn, userId);
        if (!syncExpiredPayLog(payLog) && syncChannel) {
            syncChannelPayStatus(payLog);
        }
        return toPayStatus(payLogMapper.selectById(payLog.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockConfirm(String paySn) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsPayLog payLog = requireMyPayLog(paySn, userId);
        PayTypeEnum payType = PayTypeEnum.fromCode(payLog.getPayType());
        if (payType != PayTypeEnum.WECHAT_MOCK) {
            throw AppException.badRequest("仅微信Mock通道支持模拟确认");
        }
        if (syncExpiredPayLog(payLog)) {
            throw AppException.conflict("支付已过期");
        }
        if (payLog.getPayStatus() != null && payLog.getPayStatus() == PayStatusEnum.SUCCESS.getCode()) {
            return;
        }
        if (payLog.getPayStatus() == null || payLog.getPayStatus() != PayStatusEnum.PENDING.getCode()) {
            throw AppException.conflict("当前支付状态不允许确认");
        }
        completePayment(payLog, "MOCK_TRADE_" + payLog.getPaySn(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        PayStrategy strategy = strategyFactory.getStrategy(PayTypeEnum.ALIPAY);
        PayCallbackResult callback = strategy.verifyCallback(params);
        if (!callback.isVerified()) {
            log.warn("支付宝回调验签失败:{}", callback.getErrorMsg());
            return "fail";
        }
        if (StrUtil.isNotBlank(alipayAppId) && !alipayAppId.equals(callback.getAppId())) {
            log.warn("支付宝回调app_id不匹配:{}", callback.getAppId());
            return "fail";
        }
        if (!"TRADE_SUCCESS".equals(callback.getTradeStatus())
                && !"TRADE_FINISHED".equals(callback.getTradeStatus())) {
            log.info("支付宝回调交易状态非成功:{}", callback.getTradeStatus());
            return "success";
        }

        OmsPayLog payLog = payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPaySn, callback.getPaySn())
                .last("limit 1"));
        if (payLog == null) {
            log.warn("支付宝回调找不到支付流水:{}", callback.getPaySn());
            return "fail";
        }
        if (!isCallbackAmountValid(payLog, callback)) {
            if (payLog.getPayStatus() != null && payLog.getPayStatus() == PayStatusEnum.PENDING.getCode()) {
                failPayLog(payLog.getId(), "Alipay callback amount mismatch");
            }
            return "fail";
        }
        if (payLog.getPayStatus() != null && payLog.getPayStatus() == PayStatusEnum.SUCCESS.getCode()) {
            if (!isCallbackTradeNoValid(payLog, callback)) {
                return "fail";
            }
            return "success";
        }
        if (payLog.getPayStatus() == null || payLog.getPayStatus() != PayStatusEnum.PENDING.getCode()) {
            log.warn("支付宝回调流水状态异常:paySn={},status={}", payLog.getPaySn(), payLog.getPayStatus());
            return "fail";
        }
        if (callback.getTotalAmount() == null
                || callback.getTotalAmount().compareTo(payLog.getPayAmount()) != 0) {
            failPayLog(payLog.getId(), "支付宝回调金额不一致");
            return "fail";
        }
        completePayment(payLog, callback.getTradeNo(), JSON.toJSONString(params));
        return "success";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OmsRefundLog refundOrder(Long orderId, Long afterSaleId, String reason) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw AppException.notFound("订单不存在");
        }
        return refundOrder(orderId, afterSaleId, order.getTotalAmount(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OmsRefundLog refundOrder(Long orderId, Long afterSaleId, BigDecimal refundAmount, String reason) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw AppException.notFound("订单不存在");
        }
        BigDecimal amount = normalizeRefundAmount(refundAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.badRequest("退款金额必须大于0");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.conflict("退款金额超过订单可退金额");
        }
        if (afterSaleId != null) {
            lockAfterSaleForRefund(afterSaleId, order.getId());
            Long existingCount = refundLogMapper.selectCount(new LambdaQueryWrapper<OmsRefundLog>()
                    .eq(OmsRefundLog::getAfterSaleId, afterSaleId)
                    .in(OmsRefundLog::getRefundStatus,
                            RefundStatusEnum.PENDING.getCode(),
                            RefundStatusEnum.SUCCESS.getCode()));
            if (existingCount != null && existingCount > 0) {
                throw AppException.conflict("该售后单已发起退款,请勿重复处理");
            }
        }

        OmsPayLog payLog = findLatestSuccessLog(order.getId());
        OmsRefundLog refundLog = new OmsRefundLog();
        refundLog.setRefundSn(nextSn("REF"));
        refundLog.setOrderId(order.getId());
        refundLog.setOrderSn(order.getOrderSn());
        refundLog.setPayLogId(payLog == null ? null : payLog.getId());
        refundLog.setAfterSaleId(afterSaleId);
        refundLog.setUserId(order.getUserId());
        refundLog.setPayType(payLog == null ? null : payLog.getPayType());
        refundLog.setRefundAmount(amount);
        refundLog.setRefundReason(StrUtil.blankToDefault(reason, "管理员同意退款"));
        refundLog.setRefundStatus(RefundStatusEnum.PENDING.getCode());
        refundLog.setCreateTime(LocalDateTime.now());
        refundLogMapper.insert(refundLog);
        reserveOrderRefundAmount(order.getId(), amount);

        if (payLog == null || payLog.getPayType() == null) {
            markRefundSuccess(refundLog.getId(), "LEGACY_REFUND_" + refundLog.getRefundSn());
            return refundLogMapper.selectById(refundLog.getId());
        }

        PayTypeEnum payType = PayTypeEnum.fromCode(payLog.getPayType());
        RefundChannelResponse response = strategyFactory.getStrategy(payType).refund(RefundChannelRequest.builder()
                .paySn(payLog.getPaySn())
                .refundSn(refundLog.getRefundSn())
                .tradeNo(payLog.getTradeNo())
                .orderSn(order.getOrderSn())
                .refundAmount(amount)
                .reason(refundLog.getRefundReason())
                .payType(payType)
                .build());
        if (!response.isSuccess()) {
            OmsRefundLog update = new OmsRefundLog();
            update.setId(refundLog.getId());
            update.setRefundStatus(RefundStatusEnum.FAILED.getCode());
            update.setErrorMsg(response.getErrorMsg());
            update.setUpdateTime(LocalDateTime.now());
            refundLogMapper.updateById(update);
            throw AppException.badRequest(StrUtil.blankToDefault(response.getErrorMsg(), "退款失败"));
        }
        markRefundSuccess(refundLog.getId(), response.getTradeNo());
        return refundLogMapper.selectById(refundLog.getId());
    }

    private BigDecimal normalizeRefundAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void lockAfterSaleForRefund(Long afterSaleId, Long orderId) {
        OmsAfterSale afterSale = afterSaleMapper.selectOne(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, afterSaleId)
                .eq(OmsAfterSale::getOrderId, orderId)
                .last("limit 1 for update"));
        if (afterSale == null) {
            throw AppException.notFound("售后单不存在");
        }
    }

    private OmsOrder requirePayableOrder(PayCreateDTO dto, Long userId) {
        if (dto == null || (dto.getOrderId() == null && StrUtil.isBlank(dto.getOrderSn()))) {
            throw AppException.badRequest("缺少订单标识");
        }
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getUserId, userId);
        if (dto.getOrderId() != null) {
            wrapper.eq(OmsOrder::getId, dto.getOrderId());
        } else {
            wrapper.eq(OmsOrder::getOrderSn, dto.getOrderSn());
        }
        wrapper.last("limit 1");
        OmsOrder order = orderMapper.selectOne(wrapper);
        if (order == null || Integer.valueOf(OmsOrder.DELETE_DELETED).equals(order.getUserDeleted())) {
            throw AppException.notFound("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_PENDING_PAY) {
            throw AppException.conflict("当前订单状态不允许支付");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.badRequest("订单金额异常");
        }
        return order;
    }

    private OmsPayLog requireMyPayLog(String paySn, Long userId) {
        if (StrUtil.isBlank(paySn)) {
            throw AppException.badRequest("缺少支付流水号");
        }
        OmsPayLog payLog = payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPaySn, paySn)
                .eq(OmsPayLog::getUserId, userId)
                .last("limit 1"));
        if (payLog == null) {
            throw AppException.notFound("支付流水不存在");
        }
        return payLog;
    }

    private OmsPayLog findLatestPendingLog(Long orderId, PayTypeEnum payType) {
        return payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getOrderId, orderId)
                .eq(OmsPayLog::getPayType, payType.getCode())
                .eq(OmsPayLog::getPayStatus, PayStatusEnum.PENDING.getCode())
                .orderByDesc(OmsPayLog::getCreateTime)
                .last("limit 1"));
    }

    private OmsPayLog findLatestSuccessLog(Long orderId) {
        return payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getOrderId, orderId)
                .eq(OmsPayLog::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                .orderByDesc(OmsPayLog::getPayTime)
                .last("limit 1"));
    }

    private boolean isCallbackAmountValid(OmsPayLog payLog, PayCallbackResult callback) {
        return callback.getTotalAmount() != null
                && payLog.getPayAmount() != null
                && callback.getTotalAmount().compareTo(payLog.getPayAmount()) == 0;
    }

    private boolean isCallbackTradeNoValid(OmsPayLog payLog, PayCallbackResult callback) {
        return StrUtil.isBlank(payLog.getTradeNo())
                || StrUtil.isBlank(callback.getTradeNo())
                || StrUtil.equals(payLog.getTradeNo(), callback.getTradeNo());
    }

    private void closePayLog(Long id, String reason) {
        OmsPayLog update = new OmsPayLog();
        update.setId(id);
        update.setPayStatus(PayStatusEnum.CLOSED.getCode());
        update.setErrorMsg(reason);
        update.setUpdateTime(LocalDateTime.now());
        payLogMapper.updateById(update);
    }

    private void failPayLog(Long id, String reason) {
        OmsPayLog update = new OmsPayLog();
        update.setId(id);
        update.setPayStatus(PayStatusEnum.FAILED.getCode());
        update.setErrorMsg(StrUtil.blankToDefault(reason, "支付失败"));
        update.setUpdateTime(LocalDateTime.now());
        payLogMapper.updateById(update);
    }

    private boolean syncExpiredPayLog(OmsPayLog payLog) {
        if (payLog == null || payLog.getPayStatus() == null
                || payLog.getPayStatus() != PayStatusEnum.PENDING.getCode()
                || payLog.getExpireTime() == null
                || payLog.getExpireTime().isAfter(LocalDateTime.now())) {
            return false;
        }
        closePayLog(payLog.getId(), "支付流水已过期");
        payLog.setPayStatus(PayStatusEnum.CLOSED.getCode());
        return true;
    }

    private void syncChannelPayStatus(OmsPayLog payLog) {
        if (payLog == null || payLog.getPayStatus() == null
                || payLog.getPayStatus() != PayStatusEnum.PENDING.getCode()) {
            return;
        }
        PayTypeEnum payType = PayTypeEnum.fromCode(payLog.getPayType());
        PayQueryResult result = strategyFactory.getStrategy(payType).queryPay(PayChannelRequest.builder()
                .paySn(payLog.getPaySn())
                .orderSn(payLog.getOrderSn())
                .orderId(payLog.getOrderId())
                .userId(payLog.getUserId())
                .payType(payType)
                .payMode(PayModeEnum.QR_CODE)
                .amount(payLog.getPayAmount())
                .expireTime(payLog.getExpireTime())
                .build());
        if (result == null || !result.isSuccess()) {
            if (result != null && StrUtil.isNotBlank(result.getErrorMsg())) {
                log.debug("支付主动查单未更新状态:paySn={},msg={}", payLog.getPaySn(), result.getErrorMsg());
            }
            return;
        }
        if (result.isClosed()) {
            closePayLog(payLog.getId(), "支付渠道交易已关闭");
            payLog.setPayStatus(PayStatusEnum.CLOSED.getCode());
            return;
        }
        if (!result.isPaid()) {
            return;
        }
        if (result.getTotalAmount() == null || result.getTotalAmount().compareTo(payLog.getPayAmount()) != 0) {
            failPayLog(payLog.getId(), "支付渠道查单金额不一致");
            payLog.setPayStatus(PayStatusEnum.FAILED.getCode());
            return;
        }
        if (completePayment(payLog, result.getTradeNo(), result.getRawContent())) {
            payLog.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        } else {
            payLog.setPayStatus(PayStatusEnum.CLOSED.getCode());
        }
    }

    private boolean completePayment(OmsPayLog payLog, String tradeNo, String callbackContent) {
        String normalizedTradeNo = StrUtil.isBlank(tradeNo) ? null : tradeNo;
        if (normalizedTradeNo != null && isTradeNoRecordedByOtherLog(payLog, normalizedTradeNo)) {
            closePayLog(payLog.getId(), "Third-party trade number already recorded by another pay log");
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean paid = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, payLog.getOrderId())
                .eq(OmsOrder::getStatus, OmsOrder.STATUS_PENDING_PAY)
                .set(OmsOrder::getStatus, OmsOrder.STATUS_PAID)
                .set(OmsOrder::getPayType, payLog.getPayType())
                .set(OmsOrder::getPaySn, payLog.getPaySn())
                .set(OmsOrder::getTradeNo, normalizedTradeNo)
                .set(OmsOrder::getPayTime, now)) > 0;

        if (!paid) {
            OmsOrder order = orderMapper.selectOne(new LambdaQueryWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, payLog.getOrderId())
                    .last("limit 1 for update"));
            if (isPaidByCurrentLog(order, payLog)) {
                markPaySuccess(payLog, normalizedTradeNo, callbackContent, now);
                return true;
            }
            if (order != null && order.getStatus() != null && order.getStatus() == OmsOrder.STATUS_PAID) {
                closePayLog(payLog.getId(), "Order already paid by another pay log");
                return false;
            }
            throw AppException.conflict("订单状态已变化,支付结果无法入账");
        }

        markPaySuccess(payLog, normalizedTradeNo, callbackContent, now);
        closeOtherPendingLogs(payLog);
        increaseProductSales(payLog.getOrderId());
        return true;
    }

    private boolean isPaidByCurrentLog(OmsOrder order, OmsPayLog payLog) {
        return order != null
                && order.getStatus() != null
                && order.getStatus() == OmsOrder.STATUS_PAID
                && StrUtil.equals(order.getPaySn(), payLog.getPaySn());
    }

    private boolean isTradeNoRecordedByOtherLog(OmsPayLog payLog, String tradeNo) {
        OmsPayLog existing = payLogMapper.selectOne(new LambdaQueryWrapper<OmsPayLog>()
                .eq(OmsPayLog::getPayType, payLog.getPayType())
                .eq(OmsPayLog::getTradeNo, tradeNo)
                .last("limit 1"));
        return existing != null && !existing.getId().equals(payLog.getId());
    }

    private void markPaySuccess(OmsPayLog payLog, String tradeNo, String callbackContent, LocalDateTime payTime) {
        OmsPayLog update = new OmsPayLog();
        update.setId(payLog.getId());
        update.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        update.setTradeNo(tradeNo);
        update.setPayTime(payTime);
        update.setCallbackTime(callbackContent == null ? null : LocalDateTime.now());
        update.setCallbackContent(callbackContent);
        update.setUpdateTime(LocalDateTime.now());
        payLogMapper.updateById(update);
    }

    private void closeOtherPendingLogs(OmsPayLog paidLog) {
        payLogMapper.update(null, new LambdaUpdateWrapper<OmsPayLog>()
                .eq(OmsPayLog::getOrderId, paidLog.getOrderId())
                .ne(OmsPayLog::getId, paidLog.getId())
                .eq(OmsPayLog::getPayStatus, PayStatusEnum.PENDING.getCode())
                .set(OmsPayLog::getPayStatus, PayStatusEnum.CLOSED.getCode())
                .set(OmsPayLog::getErrorMsg, "订单已通过其他流水支付")
                .set(OmsPayLog::getUpdateTime, LocalDateTime.now()));
    }

    private void increaseProductSales(Long orderId) {
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0 || item.getProductId() == null) {
                continue;
            }
            productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, item.getProductId())
                    .setSql("sales = sales + " + qty));
        }
    }

    private void markRefundSuccess(Long refundLogId, String tradeNo) {
        OmsRefundLog update = new OmsRefundLog();
        update.setId(refundLogId);
        update.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
        update.setTradeNo(tradeNo);
        update.setRefundTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        refundLogMapper.updateById(update);
    }

    private void reserveOrderRefundAmount(Long orderId, BigDecimal amount) {
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                .eq(OmsOrder::getId, orderId)
                .apply("IFNULL(refund_amount, 0) + {0} <= total_amount", amount)
                .setSql("refund_amount = IFNULL(refund_amount, 0) + " + amount.toPlainString()));
        if (updated <= 0) {
            throw AppException.conflict("退款金额超过订单可退金额");
        }
    }

    private PayResponseVO toPayResponse(OmsPayLog payLog) {
        PayResponseVO vo = new PayResponseVO();
        vo.setPaySn(payLog.getPaySn());
        vo.setOrderSn(payLog.getOrderSn());
        vo.setPayType(PayTypeEnum.fromCode(payLog.getPayType()).name());
        vo.setPayStatus(PayStatusEnum.fromCode(payLog.getPayStatus()).name());
        vo.setAmount(payLog.getPayAmount());
        vo.setQrCodeUrl(payLog.getQrCodeUrl());
        vo.setExpireTime(payLog.getExpireTime());
        vo.setSandbox(PayTypeEnum.ALIPAY.getCode() == payLog.getPayType() && isAlipaySandbox());
        return vo;
    }

    private PayStatusVO toPayStatus(OmsPayLog payLog) {
        PayStatusVO vo = new PayStatusVO();
        vo.setPaySn(payLog.getPaySn());
        vo.setOrderSn(payLog.getOrderSn());
        vo.setStatus(PayStatusEnum.fromCode(payLog.getPayStatus()).name());
        vo.setPayTime(payLog.getPayTime());
        return vo;
    }

    private int resolveTimeoutMinutes() {
        return payTimeoutMinutes == null || payTimeoutMinutes <= 0 ? 30 : payTimeoutMinutes;
    }

    private String nextSn(String prefix) {
        return prefix + LocalDateTime.now().format(SN_TIME_FORMATTER)
                + IdUtil.simpleUUID().substring(0, 8).toUpperCase();
    }

    private boolean isAlipaySandbox() {
        return StrUtil.isNotBlank(alipayGatewayUrl)
                && (alipayGatewayUrl.contains("sandbox") || alipayGatewayUrl.contains("alipaydev"));
    }
}
