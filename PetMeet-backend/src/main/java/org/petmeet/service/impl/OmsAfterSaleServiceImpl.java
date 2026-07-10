package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.AfterSaleApplyDTO;
import org.petmeet.dto.AfterSaleReturnLogisticsDTO;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsAfterSaleLog;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.mapper.OmsAfterSaleLogMapper;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.service.OmsAfterSaleService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AfterSaleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OmsAfterSaleServiceImpl extends ServiceImpl<OmsAfterSaleMapper, OmsAfterSale> implements OmsAfterSaleService {

    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsAfterSaleLogMapper afterSaleLogMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final SysNotificationService notificationService;

    /**
     * 申请售后
     */
    @Override
    public Long apply(AfterSaleApplyDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 查询订单并校验归属
        OmsOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw AppException.notFound("未找到订单"); }
        if (order.getStatus() == null
                || order.getStatus() == OmsOrder.STATUS_PENDING_PAY
                || order.getStatus() == OmsOrder.STATUS_CLOSED
                || order.getStatus() == OmsOrder.STATUS_REFUNDING) {
            throw AppException.badRequest("当前订单状态不允许售后");}
        OmsOrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        if (item == null || !Objects.equals(item.getOrderId(), order.getId())) {
            throw AppException.notFound("未找到订单项"); }
        Integer type = dto.getType();
        if (type == null || (type != 0 && type != 1 && type != 2)) {
            throw AppException.badRequest("售后类型无效");  }
        Long activeCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getUserId, userId)
                .eq(OmsAfterSale::getOrderItemId, dto.getOrderItemId())
                .in(OmsAfterSale::getStatus, OmsAfterSale.activeStatuses()));
        if (activeCount != null && activeCount > 0) {
            throw AppException.badRequest("此商品已存在有效的售后请求"); }
        // 保存售后申请
        OmsAfterSale afterSale = new OmsAfterSale(); afterSale.setOrderId(order.getId());
        afterSale.setOrderItemId(item.getId()); afterSale.setUserId(userId);
        afterSale.setType(type);afterSale.setReason(StrUtil.trimToNull(dto.getReason()));
        afterSale.setDescription(StrUtil.trimToNull(dto.getDescription()));
        afterSale.setEvidenceImages(toEvidenceJson(dto.getEvidenceImages()));
        afterSale.setRefundAmount(calculateItemAmount(item));
        afterSale.setStatus(OmsAfterSale.STATUS_PENDING);
        afterSale.setCreateTime(LocalDateTime.now());
        afterSaleMapper.insert(afterSale);
        insertLog(afterSale.getId(), null, OmsAfterSale.STATUS_PENDING,
                "apply", "user", userId, "用户提交售后申请");
        notificationService.sendToUser(
                userId,"售后申请已提交",  buildApplyNotificationContent(order.getOrderSn(), type),
                "after_sale",
                afterSale.getId()  );
        return afterSale.getId();
    }

    /**
     * 我的售后列表
     */
    @Override
    public Page<AfterSaleVO> pageMy(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<OmsAfterSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OmsAfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsAfterSale::getUserId, userId);
        wrapper.and(w -> w.isNull(OmsAfterSale::getUserDeleted).or().eq(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(OmsAfterSale::getStatus, status);
        }
        wrapper.orderByDesc(OmsAfterSale::getCreateTime);
        afterSaleMapper.selectPage(page, wrapper);

        List<OmsAfterSale> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        // 批量查询订单和商品项
        Set<Long> orderIds = records.stream().map(OmsAfterSale::getOrderId).collect(Collectors.toSet());
        Set<Long> itemIds = records.stream().map(OmsAfterSale::getOrderItemId).collect(Collectors.toSet());
        Map<Long, OmsOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(OmsOrder::getId, o -> o, (a, b) -> a));
        Map<Long, OmsOrderItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : orderItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(OmsOrderItem::getId, i -> i, (a, b) -> a));

        // 组装售后展示数据
        List<AfterSaleVO> voList = new ArrayList<>();
        for (OmsAfterSale record : records) {
            AfterSaleVO vo = new AfterSaleVO();
            vo.setId(record.getId());
            vo.setOrderId(record.getOrderId());
            vo.setOrderItemId(record.getOrderItemId());
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
            }

            voList.add(vo);
        }

        Page<AfterSaleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 取消售后
     */
    @Override
    public void cancel(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getUserDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw AppException.forbidden("无权操作该售后请求");
        }
        if (afterSale.getStatus() == null
                || (afterSale.getStatus() != OmsAfterSale.STATUS_PENDING
                && afterSale.getStatus() != OmsAfterSale.STATUS_PROCESSING
                && afterSale.getStatus() != OmsAfterSale.STATUS_WAIT_BUYER_RETURN)) {
            throw AppException.conflict("当前售后状态不允许取消");
        }
        if (afterSale.getStatus() == OmsAfterSale.STATUS_PROCESSING && afterSale.getReturnReceiveTime() != null) {
            throw AppException.conflict("当前售后状态不允许取消");
        }
        int updated = afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .eq(OmsAfterSale::getUserId, userId)
                .and(w -> w.isNull(OmsAfterSale::getUserDeleted)
                        .or().eq(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_VISIBLE))
                .in(OmsAfterSale::getStatus,
                        OmsAfterSale.STATUS_PENDING,
                        OmsAfterSale.STATUS_PROCESSING,
                        OmsAfterSale.STATUS_WAIT_BUYER_RETURN)
                .and(w -> w.ne(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PROCESSING)
                        .or().isNull(OmsAfterSale::getReturnReceiveTime))
                .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_CANCELED)
                .set(OmsAfterSale::getHandleRemark, "用户已取消售后申请")
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
        insertLog(id, afterSale.getStatus(), OmsAfterSale.STATUS_CANCELED,
                "cancel", "user", userId, "用户已取消售后申请");
    }

    /**
     * 确认售后完成
     */
    @Override
    public void complete(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getUserDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw AppException.forbidden("无权操作该售后请求");
        }
        if (afterSale.getStatus() == null || afterSale.getStatus() != OmsAfterSale.STATUS_EXCHANGE_SHIPPED) {
            throw AppException.conflict("仅换货已发货后允许确认完成");
        }
        int updated = afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .eq(OmsAfterSale::getUserId, userId)
                .and(w -> w.isNull(OmsAfterSale::getUserDeleted)
                        .or().eq(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_VISIBLE))
                .eq(OmsAfterSale::getStatus, OmsAfterSale.STATUS_EXCHANGE_SHIPPED)
                .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_COMPLETED)
                .set(OmsAfterSale::getHandleRemark, "用户已确认售后完成")
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
        insertLog(id, afterSale.getStatus(), OmsAfterSale.STATUS_COMPLETED,
                "complete", "user", userId, "用户已确认售后完成");
    }

    /**
     * 删除我的售后
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMy(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getUserDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw AppException.forbidden("无权操作该售后请求");
        }

        Integer status = afterSale.getStatus();
        if (!OmsAfterSale.isTerminalStatus(status)) {
            throw AppException.conflict("仅已完成、已拒绝或已取消的售后记录可删除");
        }

        LambdaUpdateWrapper<OmsAfterSale> wrapper = new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .eq(OmsAfterSale::getUserId, userId)
                .and(w -> w.isNull(OmsAfterSale::getUserDeleted)
                        .or().eq(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_VISIBLE))
                .in(OmsAfterSale::getStatus,
                        OmsAfterSale.STATUS_COMPLETED,
                        OmsAfterSale.STATUS_REJECTED,
                        OmsAfterSale.STATUS_CANCELED)
                .set(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_DELETED);

        int updated = afterSaleMapper.update(null, wrapper);
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReturnLogistics(Long id, AfterSaleReturnLogisticsDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getUserDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }
        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw AppException.forbidden("无权操作该售后请求");
        }
        if (afterSale.getStatus() == null || afterSale.getStatus() != OmsAfterSale.STATUS_WAIT_BUYER_RETURN) {
            throw AppException.conflict("当前售后状态不允许填写退货物流");
        }
        String company = StrUtil.trimToNull(dto.getCompany());
        String trackingNo = StrUtil.trimToNull(dto.getTrackingNo());
        int updated = afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .eq(OmsAfterSale::getUserId, userId)
                .and(w -> w.isNull(OmsAfterSale::getUserDeleted)
                        .or().eq(OmsAfterSale::getUserDeleted, OmsAfterSale.DELETE_VISIBLE))
                .eq(OmsAfterSale::getStatus, OmsAfterSale.STATUS_WAIT_BUYER_RETURN)
                .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE)
                .set(OmsAfterSale::getReturnCompany, company)
                .set(OmsAfterSale::getReturnTrackingNo, trackingNo)
                .set(OmsAfterSale::getReturnShipTime, LocalDateTime.now())
                .set(OmsAfterSale::getHandleRemark, "用户已提交退货物流")
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
        insertLog(id, afterSale.getStatus(), OmsAfterSale.STATUS_WAIT_MERCHANT_RECEIVE,
                "return_logistics", "user", userId, "用户提交退货物流：" + company + " " + trackingNo);
    }

    private String getTypeDesc(Integer type) {
        if (type == null) {
            return "售后";
        }
        return switch (type) {
            case 0 -> "仅退款";
            case 1 -> "退货退款";
            case 2 -> "换货";
            default -> "售后";
        };
    }

    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "处理中";
        }
        return switch (status) {
            case 0 -> "申请中";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "已拒绝";
            case 4 -> "已取消";
            case 5 -> "待买家退货";
            case 6 -> "待商家收货";
            case 7 -> "退款中";
            case 8 -> "换货已发货";
            default -> "处理中";
        };
    }

    private BigDecimal calculateItemAmount(OmsOrderItem item) {
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
        return price.multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP);
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

    private String buildApplyNotificationContent(String orderSn, Integer type) {
        String typeText = getTypeDesc(type);
        String orderText = StrUtil.isBlank(orderSn) ? "你的订单" : ("订单" + orderSn);
        return orderText + "已提交" + typeText + "申请，申请中请耐心等待处理。";
    }

    private String toEvidenceJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> normalized = images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(9)
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : JSON.toJSONString(normalized);
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
