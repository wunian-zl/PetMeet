package org.petmeet.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.AppException;
import org.petmeet.dto.AdminAfterSaleActionDTO;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsAfterSaleLog;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.OmsRefundLog;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.enums.RefundStatusEnum;
import org.petmeet.mapper.OmsAfterSaleLogMapper;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsRefundLogMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.AdminAfterSaleService;
import org.petmeet.service.PayService;
import org.petmeet.service.PmsProductService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AdminAfterSaleVO;
import org.petmeet.vo.AfterSaleLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAfterSaleServiceImpl implements AdminAfterSaleService {

    private static final int TYPE_REFUND_ONLY = 0;
    private static final int TYPE_RETURN_REFUND = 1;
    private static final int TYPE_EXCHANGE = 2;

    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsAfterSaleLogMapper afterSaleLogMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final OmsRefundLogMapper refundLogMapper;
    private final SysUserMapper userMapper;
    private final PmsProductService productService;
    private final PayService payService;
    private final SysNotificationService notificationService;

    @Override
    public Page<AdminAfterSaleVO> pageList(Integer pageNum, Integer pageSize, Integer status, Integer type, String keyword) {
        Page<OmsAfterSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OmsAfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(OmsAfterSale::getAdminDeleted)
                .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(OmsAfterSale::getStatus, status);
        }
        if (type != null) {
            wrapper.eq(OmsAfterSale::getType, type);
        }

        if (StrUtil.isNotBlank(keyword)) {
            applyKeywordFilter(wrapper, keyword.trim());
        }

        wrapper.orderByDesc(OmsAfterSale::getCreateTime);
        afterSaleMapper.selectPage(page, wrapper);

        List<OmsAfterSale> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        List<AdminAfterSaleVO> voList = buildVOList(records, false);
        Page<AdminAfterSaleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public AdminAfterSaleVO detail(Long id) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        return buildVOList(List.of(afterSale), true).get(0);
    }

    /**
     * 旧接口保留兼容。新工作台使用明确动作接口。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String remark) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        OmsOrder order = requireOrder(afterSale.getOrderId());
        if (order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_REFUNDING) {
            throw AppException.conflict("旧状态接口仅支持订单退款兼容场景,请使用售后工作台动作接口");
        }
        if (status != null && status == OmsAfterSale.STATUS_COMPLETED) {
            throw AppException.conflict("旧状态接口不支持完成退款,请使用售后工作台动作接口");
        }
        if (status == null || (status != OmsAfterSale.STATUS_PROCESSING
                && status != OmsAfterSale.STATUS_REJECTED)) {
            throw AppException.badRequest("售后状态无效");
        }
        if (OmsAfterSale.isTerminalStatus(afterSale.getStatus())) {
            throw AppException.conflict("当前状态无法更改");
        }
        transition(afterSale, status, "legacy_status_update", "admin", currentOperatorId(), remark);
        syncRefundingOrderLegacyState(afterSale, status, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        requireType(afterSale, TYPE_REFUND_ONLY, "仅退款申请才能直接退款");
        requireStatusIn(afterSale, "当前状态不能处理退款", OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING);
        processRefund(afterSale, remark(dto, "管理员已同意退款"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturn(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        if (afterSale.getType() == null || (afterSale.getType() != TYPE_RETURN_REFUND && afterSale.getType() != TYPE_EXCHANGE)) {
            throw AppException.badRequest("仅退货退款或换货申请需要买家退货");
        }
        requireStatusIn(afterSale, "当前状态不能同意退货", OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING);
        String address = trimToNull(dto == null ? null : dto.getReturnAddress());
        if (StrUtil.isBlank(address)) {
            throw AppException.badRequest("请填写退货地址");
        }
        updateAfterSale(afterSale, OmsAfterSale.STATUS_WAIT_BUYER_RETURN,
                "approve_return", remark(dto, "管理员已同意退货"), w -> w.set(OmsAfterSale::getReturnAddress, address));
        sendNotice(afterSale, "售后申请已通过", "请按售后详情中的退货地址寄回商品，并填写退货物流。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnRefund(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        requireType(afterSale, TYPE_RETURN_REFUND, "仅退货退款申请可以确认收货并退款");
        requireStatusIn(afterSale, "买家退货物流提交后才能确认收货", OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE);
        updateAfterSale(afterSale, OmsAfterSale.STATUS_REFUNDING,
                "confirm_return_refund", remark(dto, "商家已确认收到退货"), w -> w.set(OmsAfterSale::getReturnReceiveTime, LocalDateTime.now()));
        OmsAfterSale latest = afterSaleMapper.selectById(afterSale.getId());
        processRefund(latest, remark(dto, "商家已确认退货并退款"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnExchange(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        requireType(afterSale, TYPE_EXCHANGE, "仅换货申请可以确认收货后换货");
        requireStatusIn(afterSale, "买家退货物流提交后才能确认收货", OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE);
        updateAfterSale(afterSale, OmsAfterSale.STATUS_PROCESSING,
                "confirm_return_exchange", remark(dto, "商家已确认收到退货，待换货发出"),
                w -> w.set(OmsAfterSale::getReturnReceiveTime, LocalDateTime.now()));
        sendNotice(afterSale, "退货已签收", "商家已确认收到退货，正在安排换货发货。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipExchange(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        requireType(afterSale, TYPE_EXCHANGE, "仅换货申请可以换货发货");
        requireStatusIn(afterSale, "当前状态不能换货发货", OmsAfterSale.STATUS_PROCESSING);
        if (afterSale.getReturnReceiveTime() == null) {
            throw AppException.conflict("商家确认收到退货后才能换货发货");
        }
        String company = trimToNull(dto == null ? null : dto.getExchangeCompany());
        String trackingNo = trimToNull(dto == null ? null : dto.getExchangeTrackingNo());
        if (StrUtil.isBlank(company) || StrUtil.isBlank(trackingNo)) {
            throw AppException.badRequest("请填写换货物流公司和单号");
        }
        updateAfterSale(afterSale, OmsAfterSale.STATUS_EXCHANGE_SHIPPED,
                "ship_exchange", remark(dto, "换货商品已发出"), w -> w
                        .set(OmsAfterSale::getExchangeCompany, company)
                        .set(OmsAfterSale::getExchangeTrackingNo, trackingNo)
                        .set(OmsAfterSale::getExchangeShipTime, LocalDateTime.now()));
        sendNotice(afterSale, "换货已发货", "你的换货商品已发出，请留意物流并在收到后确认完成。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        if (OmsAfterSale.isTerminalStatus(afterSale.getStatus()) || afterSale.getStatus() == OmsAfterSale.STATUS_REFUNDING) {
            throw AppException.conflict("当前状态不能拒绝");
        }
        String remark = remark(dto, "管理员已拒绝售后申请");
        transition(afterSale, OmsAfterSale.STATUS_REJECTED, "reject", "admin", currentOperatorId(), remark);
        restoreRefundingOrderIfNeeded(afterSale);
        sendNotice(afterSale, "售后申请已驳回", remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long id) {
        OmsAfterSale afterSale = requireVisibleAfterSale(id);
        if (!canSoftDeleteStatus(afterSale.getStatus())) {
            throw AppException.badRequest("仅已完成、已拒绝或已取消的售后记录可删除");
        }

        afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .and(w -> w.isNull(OmsAfterSale::getAdminDeleted)
                        .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE))
                .set(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_DELETED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .forEach(this::softDelete);
    }

    private void applyKeywordFilter(LambdaQueryWrapper<OmsAfterSale> wrapper, String keyword) {
        List<Long> orderIds = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                        .select(OmsOrder::getId)
                        .like(OmsOrder::getOrderSn, keyword))
                .stream().map(OmsOrder::getId).toList();

        List<Long> itemIds = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                        .select(OmsOrderItem::getId)
                        .like(OmsOrderItem::getProductName, keyword))
                .stream().map(OmsOrderItem::getId).toList();

        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword)))
                .stream().map(SysUser::getId).toList();

        wrapper.and(w -> {
            boolean hasCondition = false;
            if (!orderIds.isEmpty()) {
                w.in(OmsAfterSale::getOrderId, orderIds);
                hasCondition = true;
            }
            if (!itemIds.isEmpty()) {
                if (hasCondition) w.or();
                w.in(OmsAfterSale::getOrderItemId, itemIds);
                hasCondition = true;
            }
            if (!userIds.isEmpty()) {
                if (hasCondition) w.or();
                w.in(OmsAfterSale::getUserId, userIds);
                hasCondition = true;
            }
            if (!hasCondition) {
                w.eq(OmsAfterSale::getId, -1);
            }
        });
    }

    private List<AdminAfterSaleVO> buildVOList(List<OmsAfterSale> records, boolean withLogs) {
        Set<Long> orderIds = records.stream().map(OmsAfterSale::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = records.stream().map(OmsAfterSale::getOrderItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(OmsAfterSale::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> afterSaleIds = records.stream().map(OmsAfterSale::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, OmsOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream().collect(Collectors.toMap(OmsOrder::getId, o -> o, (a, b) -> a));
        Map<Long, OmsOrderItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : orderItemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(OmsOrderItem::getId, i -> i, (a, b) -> a));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, OmsRefundLog> refundMap = afterSaleIds.isEmpty() ? Collections.emptyMap()
                : refundLogMapper.selectList(new LambdaQueryWrapper<OmsRefundLog>()
                        .in(OmsRefundLog::getAfterSaleId, afterSaleIds)
                        .orderByDesc(OmsRefundLog::getCreateTime)).stream()
                .collect(Collectors.toMap(OmsRefundLog::getAfterSaleId, r -> r, (first, ignored) -> first));
        Map<Long, List<AfterSaleLogVO>> logMap = withLogs ? loadLogMap(afterSaleIds) : Collections.emptyMap();

        List<AdminAfterSaleVO> voList = new ArrayList<>();
        for (OmsAfterSale record : records) {
            AdminAfterSaleVO vo = new AdminAfterSaleVO();
            fillBaseVO(vo, record);

            OmsOrder order = orderMap.get(record.getOrderId());
            if (order != null) {
                vo.setOrderSn(order.getOrderSn());
            }

            OmsOrderItem item = itemMap.get(record.getOrderItemId());
            if (item != null) {
                vo.setProductId(item.getProductId());
                vo.setProductName(item.getProductName());
                vo.setProductImg(item.getProductImg());
                vo.setPrice(item.getPrice());
                vo.setQuantity(item.getQuantity());
                if (vo.getRefundAmount() == null || vo.getRefundAmount().compareTo(BigDecimal.ZERO) == 0) {
                    vo.setRefundAmount(calculateItemAmount(item));
                }
            }

            SysUser user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }

            OmsRefundLog refundLog = refundMap.get(record.getId());
            if (refundLog != null) {
                vo.setRefundSn(refundLog.getRefundSn());
                vo.setRefundStatus(refundLog.getRefundStatus());
                vo.setRefundStatusDesc(getRefundStatusDesc(refundLog.getRefundStatus()));
                vo.setRefundTradeNo(refundLog.getTradeNo());
                vo.setRefundTime(refundLog.getRefundTime());
                vo.setRefundErrorMsg(refundLog.getErrorMsg());
            }
            if (withLogs) {
                vo.setLogs(logMap.getOrDefault(record.getId(), Collections.emptyList()));
            }
            voList.add(vo);
        }
        return voList;
    }

    private void fillBaseVO(AdminAfterSaleVO vo, OmsAfterSale record) {
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setOrderItemId(record.getOrderItemId());
        vo.setUserId(record.getUserId());
        vo.setType(record.getType());
        vo.setTypeDesc(getTypeDesc(record.getType()));
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(getStatusDesc(record.getStatus()));
        vo.setReason(record.getReason());
        vo.setDescription(record.getDescription());
        vo.setEvidenceImages(parseEvidence(record.getEvidenceImages()));
        vo.setRefundAmount(record.getRefundAmount());
        vo.setReturnAddress(record.getReturnAddress());
        vo.setReturnCompany(record.getReturnCompany());
        vo.setReturnTrackingNo(record.getReturnTrackingNo());
        vo.setReturnShipTime(record.getReturnShipTime());
        vo.setReturnReceiveTime(record.getReturnReceiveTime());
        vo.setExchangeCompany(record.getExchangeCompany());
        vo.setExchangeTrackingNo(record.getExchangeTrackingNo());
        vo.setExchangeShipTime(record.getExchangeShipTime());
        vo.setHandleRemark(record.getHandleRemark());
        vo.setCreateTime(record.getCreateTime());
        vo.setHandleTime(record.getHandleTime());
    }

    private Map<Long, List<AfterSaleLogVO>> loadLogMap(Set<Long> afterSaleIds) {
        if (afterSaleIds == null || afterSaleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return afterSaleLogMapper.selectList(new LambdaQueryWrapper<OmsAfterSaleLog>()
                        .in(OmsAfterSaleLog::getAfterSaleId, afterSaleIds)
                        .orderByAsc(OmsAfterSaleLog::getCreateTime))
                .stream()
                .map(this::toLogVO)
                .collect(Collectors.groupingBy(AfterSaleLogVO::getAfterSaleId));
    }

    private AfterSaleLogVO toLogVO(OmsAfterSaleLog log) {
        AfterSaleLogVO vo = new AfterSaleLogVO();
        vo.setId(log.getId());
        vo.setAfterSaleId(log.getAfterSaleId());
        vo.setFromStatus(log.getFromStatus());
        vo.setFromStatusDesc(getStatusDesc(log.getFromStatus()));
        vo.setToStatus(log.getToStatus());
        vo.setToStatusDesc(getStatusDesc(log.getToStatus()));
        vo.setAction(log.getAction());
        vo.setOperatorType(log.getOperatorType());
        vo.setOperatorId(log.getOperatorId());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private void processRefund(OmsAfterSale afterSale, String remark) {
        OmsOrder order = requireOrder(afterSale.getOrderId());
        OmsOrderItem item = requireOrderItem(afterSale.getOrderItemId(), order.getId());
        BigDecimal amount = calculateItemAmount(item);
        updateAfterSale(afterSale, OmsAfterSale.STATUS_REFUNDING, "refund_start", remark,
                w -> w.set(OmsAfterSale::getRefundAmount, amount));
        try {
            payService.refundOrder(order.getId(), afterSale.getId(), amount, remark);
        } catch (RuntimeException e) {
            transition(afterSaleMapper.selectById(afterSale.getId()), OmsAfterSale.STATUS_PROCESSING,
                    "refund_failed", "admin", currentOperatorId(), e.getMessage());
            throw e;
        }
        OmsOrder latestOrder = requireOrder(order.getId());
        rollbackItemStock(item);
        BigDecimal refunded = latestOrder.getRefundAmount() == null ? BigDecimal.ZERO : latestOrder.getRefundAmount();
        boolean fullRefund = latestOrder.getTotalAmount() != null && refunded.compareTo(latestOrder.getTotalAmount()) >= 0;
        if (fullRefund) {
            orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, latestOrder.getId())
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED));
        }
        transition(afterSaleMapper.selectById(afterSale.getId()), OmsAfterSale.STATUS_COMPLETED,
                "refund_success", "admin", currentOperatorId(), remark);
        sendNotice(afterSale, "退款已完成", "售后退款已处理完成，退款金额¥" + amount.toPlainString() + "。");
    }

    private void updateAfterSale(OmsAfterSale afterSale, Integer toStatus, String action, String remark,
                                 java.util.function.Function<LambdaUpdateWrapper<OmsAfterSale>, LambdaUpdateWrapper<OmsAfterSale>> extra) {
        Integer fromStatus = afterSale.getStatus();
        LambdaUpdateWrapper<OmsAfterSale> wrapper = new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, afterSale.getId())
                .and(w -> w.isNull(OmsAfterSale::getAdminDeleted)
                        .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE))
                .set(OmsAfterSale::getStatus, toStatus)
                .set(OmsAfterSale::getHandleRemark, StrUtil.trimToNull(remark))
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now());
        if (fromStatus == null) {
            wrapper.isNull(OmsAfterSale::getStatus);
        } else {
            wrapper.eq(OmsAfterSale::getStatus, fromStatus);
        }
        if (extra != null) {
            wrapper = extra.apply(wrapper);
        }
        int updated = afterSaleMapper.update(null, wrapper);
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
        insertLog(afterSale.getId(), fromStatus, toStatus, action, "admin", currentOperatorId(), remark);
        afterSale.setStatus(toStatus);
    }

    private void transition(OmsAfterSale afterSale, Integer toStatus, String action, String operatorType, Long operatorId, String remark) {
        updateAfterSale(afterSale, toStatus, action, remark, null);
    }

    private void insertLog(Long afterSaleId, Integer fromStatus, Integer toStatus, String action,
                           String operatorType, Long operatorId, String remark) {
        OmsAfterSaleLog log = new OmsAfterSaleLog();
        log.setAfterSaleId(afterSaleId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(StrUtil.trimToNull(remark));
        log.setCreateTime(LocalDateTime.now());
        afterSaleLogMapper.insert(log);
    }

    private OmsAfterSale requireVisibleAfterSale(Long id) {
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getAdminDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }
        return afterSale;
    }

    private OmsOrder requireOrder(Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw AppException.notFound("订单不存在");
        }
        return order;
    }

    private OmsOrderItem requireOrderItem(Long itemId, Long orderId) {
        OmsOrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getOrderId(), orderId)) {
            throw AppException.notFound("订单商品不存在");
        }
        return item;
    }

    private void requireType(OmsAfterSale afterSale, int type, String message) {
        if (afterSale.getType() == null || afterSale.getType() != type) {
            throw AppException.badRequest(message);
        }
    }

    private void requireStatusIn(OmsAfterSale afterSale, String message, Integer... statuses) {
        for (Integer status : statuses) {
            if (Objects.equals(afterSale.getStatus(), status)) {
                return;
            }
        }
        throw AppException.conflict(message);
    }

    private BigDecimal calculateItemAmount(OmsOrderItem item) {
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
        return price.multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void syncRefundingOrderLegacyState(OmsAfterSale afterSale, Integer status, String remark) {
        OmsOrder order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null || order.getStatus() == null || order.getStatus() != OmsOrder.STATUS_REFUNDING) {
            sendNotice(afterSale, buildStatusNotificationTitle(status), buildStatusNotificationContent(status, order == null ? null : order.getOrderSn(), remark));
            return;
        }
        if (status == OmsAfterSale.STATUS_REJECTED) {
            restoreRefundingOrderIfNeeded(afterSale);
        }
        if (status == OmsAfterSale.STATUS_COMPLETED) {
            orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED));
            rollbackStock(order.getId());
        }
        sendNotice(afterSale, buildStatusNotificationTitle(status), buildStatusNotificationContent(status, order.getOrderSn(), remark));
    }

    private void restoreRefundingOrderIfNeeded(OmsAfterSale afterSale) {
        OmsOrder order = orderMapper.selectById(afterSale.getOrderId());
        if (order != null && order.getStatus() != null && order.getStatus() == OmsOrder.STATUS_REFUNDING) {
            orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_PAID));
        }
    }

    private void rollbackStock(Long orderId) {
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            rollbackItemStock(item);
        }
    }

    private void rollbackItemStock(OmsOrderItem item) {
        Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
        if (item.getProductId() == null || qty <= 0) {
            return;
        }
        productService.update(new LambdaUpdateWrapper<PmsProduct>()
                .eq(PmsProduct::getId, item.getProductId())
                .setSql("stock = stock + " + qty));
    }

    private boolean canSoftDeleteStatus(Integer status) {
        return OmsAfterSale.isTerminalStatus(status);
    }

    private Long currentOperatorId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ignore) {
            return null;
        }
    }

    private String trimToNull(Object value) {
        return value == null ? null : StrUtil.trimToNull(String.valueOf(value));
    }

    private String remark(AdminAfterSaleActionDTO dto, String fallback) {
        return StrUtil.blankToDefault(trimToNull(dto == null ? null : dto.getRemark()), fallback);
    }

    private void sendNotice(OmsAfterSale afterSale, String title, String content) {
        notificationService.sendToUser(afterSale.getUserId(), title, content, "after_sale", afterSale.getId());
    }

    private String getTypeDesc(Integer type) {
        if (type == null) return "售后";
        return switch (type) {
            case TYPE_REFUND_ONLY -> "仅退款";
            case TYPE_RETURN_REFUND -> "退货退款";
            case TYPE_EXCHANGE -> "换货";
            default -> "售后";
        };
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "处理中";
        return switch (status) {
            case OmsAfterSale.STATUS_PENDING -> "申请中";
            case OmsAfterSale.STATUS_PROCESSING -> "处理中";
            case OmsAfterSale.STATUS_COMPLETED -> "已完成";
            case OmsAfterSale.STATUS_REJECTED -> "已拒绝";
            case OmsAfterSale.STATUS_CANCELED -> "已取消";
            case OmsAfterSale.STATUS_WAIT_BUYER_RETURN -> "待买家退货";
            case OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE -> "待商家收货";
            case OmsAfterSale.STATUS_REFUNDING -> "退款中";
            case OmsAfterSale.STATUS_EXCHANGE_SHIPPED -> "换货已发货";
            default -> "处理中";
        };
    }

    private String getRefundStatusDesc(Integer status) {
        if (status == null) return null;
        if (status == RefundStatusEnum.PENDING.getCode()) return "退款中";
        if (status == RefundStatusEnum.SUCCESS.getCode()) return "退款成功";
        if (status == RefundStatusEnum.FAILED.getCode()) return "退款失败";
        return "未知";
    }

    private String buildStatusNotificationTitle(Integer status) {
        return switch (status == null ? -1 : status) {
            case OmsAfterSale.STATUS_PROCESSING -> "售后处理中";
            case OmsAfterSale.STATUS_COMPLETED -> "售后处理完成";
            case OmsAfterSale.STATUS_REJECTED -> "售后申请已驳回";
            default -> "售后状态已更新";
        };
    }

    private String buildStatusNotificationContent(Integer status, String orderSn, String remark) {
        String orderText = StrUtil.isBlank(orderSn) ? "你的订单" : ("订单" + orderSn);
        String safeRemark = StrUtil.trimToNull(remark);
        return switch (status == null ? -1 : status) {
            case OmsAfterSale.STATUS_PROCESSING -> orderText + "售后申请正在处理中，请耐心等待。";
            case OmsAfterSale.STATUS_COMPLETED -> orderText + "售后申请已处理完成。";
            case OmsAfterSale.STATUS_REJECTED -> StrUtil.isBlank(safeRemark)
                    ? (orderText + "售后申请已被驳回，请查看详情。")
                    : (orderText + "售后申请已被驳回，原因：" + safeRemark);
            default -> orderText + "售后状态已更新，请留意处理进度。";
        };
    }

    private List<String> parseEvidence(String evidenceJson) {
        if (StrUtil.isBlank(evidenceJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSON.parseArray(evidenceJson, String.class);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }
}
