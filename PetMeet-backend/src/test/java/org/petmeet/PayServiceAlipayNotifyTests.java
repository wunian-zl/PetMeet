package org.petmeet;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.OmsPayLog;
import org.petmeet.entity.PmsProduct;
import org.petmeet.enums.PayModeEnum;
import org.petmeet.enums.PayStatusEnum;
import org.petmeet.enums.PayTypeEnum;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsPayLogMapper;
import org.petmeet.mapper.OmsRefundLogMapper;
import org.petmeet.pay.PayCallbackResult;
import org.petmeet.pay.strategy.PayStrategy;
import org.petmeet.pay.strategy.PayStrategyFactory;
import org.petmeet.service.PmsProductService;
import org.petmeet.service.impl.PayServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayServiceAlipayNotifyTests {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OmsOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OmsPayLog.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PmsProduct.class);
    }

    @Mock
    private OmsOrderMapper orderMapper;

    @Mock
    private OmsOrderItemMapper orderItemMapper;

    @Mock
    private OmsPayLogMapper payLogMapper;

    @Mock
    private OmsRefundLogMapper refundLogMapper;

    @Mock
    private PmsProductService productService;

    @Mock
    private PayStrategyFactory strategyFactory;

    @Mock
    private PayStrategy alipayStrategy;

    @InjectMocks
    private PayServiceImpl payService;

    @Test
    void repeatedSuccessfulNotifyDoesNotSettleTwice() {
        PayCallbackResult callback = successCallback("PAY1", "ALI_TRADE_1");
        OmsPayLog pendingLog = payLog(1L, "PAY1", PayStatusEnum.PENDING);
        OmsPayLog successLog = payLog(1L, "PAY1", PayStatusEnum.SUCCESS);
        OmsOrderItem item = new OmsOrderItem();
        item.setProductId(100L);
        item.setQuantity(2);

        when(strategyFactory.getStrategy(PayTypeEnum.ALIPAY)).thenReturn(alipayStrategy);
        when(alipayStrategy.verifyCallback(any())).thenReturn(callback);
        when(payLogMapper.selectOne(any()))
                .thenReturn(pendingLog)
                .thenReturn(null)
                .thenReturn(successLog);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productService.update(any())).thenReturn(true);

        assertEquals("success", payService.handleAlipayNotify(Map.of()));
        assertEquals("success", payService.handleAlipayNotify(Map.of()));

        verify(orderMapper, times(1)).update(isNull(), any());
        verify(productService, times(1)).update(any());
    }

    @Test
    void competingPayLogIsClosedWhenOrderWasPaidByAnotherPaySn() {
        PayCallbackResult callback = successCallback("PAY2", "ALI_TRADE_2");
        OmsPayLog competingLog = payLog(2L, "PAY2", PayStatusEnum.PENDING);
        OmsOrder paidOrder = new OmsOrder();
        paidOrder.setId(10L);
        paidOrder.setStatus(OmsOrder.STATUS_PAID);
        paidOrder.setPaySn("PAY1");

        when(strategyFactory.getStrategy(PayTypeEnum.ALIPAY)).thenReturn(alipayStrategy);
        when(alipayStrategy.verifyCallback(any())).thenReturn(callback);
        when(payLogMapper.selectOne(any()))
                .thenReturn(competingLog)
                .thenReturn(null);
        when(orderMapper.update(isNull(), any())).thenReturn(0);
        when(orderMapper.selectOne(any())).thenReturn(paidOrder);

        assertEquals("success", payService.handleAlipayNotify(Map.of()));

        ArgumentCaptor<OmsPayLog> payLogCaptor = ArgumentCaptor.forClass(OmsPayLog.class);
        verify(payLogMapper).updateById(payLogCaptor.capture());
        assertEquals(2L, payLogCaptor.getValue().getId());
        assertEquals(PayStatusEnum.CLOSED.getCode(), payLogCaptor.getValue().getPayStatus());
        verify(productService, never()).update(any());
    }

    @Test
    void duplicateTradeNoIsClosedBeforeOrderSettlement() {
        PayCallbackResult callback = successCallback("PAY2", "ALI_TRADE_1");
        OmsPayLog competingLog = payLog(2L, "PAY2", PayStatusEnum.PENDING);
        OmsPayLog existingSuccessLog = payLog(1L, "PAY1", PayStatusEnum.SUCCESS);
        existingSuccessLog.setTradeNo("ALI_TRADE_1");

        when(strategyFactory.getStrategy(PayTypeEnum.ALIPAY)).thenReturn(alipayStrategy);
        when(alipayStrategy.verifyCallback(any())).thenReturn(callback);
        when(payLogMapper.selectOne(any()))
                .thenReturn(competingLog)
                .thenReturn(existingSuccessLog);

        assertEquals("success", payService.handleAlipayNotify(Map.of()));

        ArgumentCaptor<OmsPayLog> payLogCaptor = ArgumentCaptor.forClass(OmsPayLog.class);
        verify(payLogMapper).updateById(payLogCaptor.capture());
        assertEquals(2L, payLogCaptor.getValue().getId());
        assertEquals(PayStatusEnum.CLOSED.getCode(), payLogCaptor.getValue().getPayStatus());
        assertNull(payLogCaptor.getValue().getTradeNo());
        verify(orderMapper, never()).update(isNull(), any());
        verify(productService, never()).update(any());
    }

    @Test
    void duplicateTradeNoUsedByClosedLogIsAlsoRejected() {
        PayCallbackResult callback = successCallback("PAY2", "ALI_TRADE_1");
        OmsPayLog competingLog = payLog(2L, "PAY2", PayStatusEnum.PENDING);
        OmsPayLog existingClosedLog = payLog(1L, "PAY1", PayStatusEnum.CLOSED);
        existingClosedLog.setTradeNo("ALI_TRADE_1");

        when(strategyFactory.getStrategy(PayTypeEnum.ALIPAY)).thenReturn(alipayStrategy);
        when(alipayStrategy.verifyCallback(any())).thenReturn(callback);
        when(payLogMapper.selectOne(any()))
                .thenReturn(competingLog)
                .thenReturn(existingClosedLog);

        assertEquals("success", payService.handleAlipayNotify(Map.of()));

        ArgumentCaptor<OmsPayLog> payLogCaptor = ArgumentCaptor.forClass(OmsPayLog.class);
        verify(payLogMapper).updateById(payLogCaptor.capture());
        assertEquals(2L, payLogCaptor.getValue().getId());
        assertEquals(PayStatusEnum.CLOSED.getCode(), payLogCaptor.getValue().getPayStatus());
        verify(orderMapper, never()).update(isNull(), any());
        verify(productService, never()).update(any());
    }

    private PayCallbackResult successCallback(String paySn, String tradeNo) {
        return PayCallbackResult.builder()
                .verified(true)
                .paySn(paySn)
                .tradeNo(tradeNo)
                .tradeStatus("TRADE_SUCCESS")
                .totalAmount(new BigDecimal("19.90"))
                .build();
    }

    private OmsPayLog payLog(Long id, String paySn, PayStatusEnum status) {
        OmsPayLog payLog = new OmsPayLog();
        payLog.setId(id);
        payLog.setPaySn(paySn);
        payLog.setOrderId(10L);
        payLog.setOrderSn("ORDER1");
        payLog.setUserId(20L);
        payLog.setPayType(PayTypeEnum.ALIPAY.getCode());
        payLog.setPayMode(PayModeEnum.QR_CODE.getCode());
        payLog.setPayAmount(new BigDecimal("19.90"));
        payLog.setPayStatus(status.getCode());
        return payLog;
    }
}
