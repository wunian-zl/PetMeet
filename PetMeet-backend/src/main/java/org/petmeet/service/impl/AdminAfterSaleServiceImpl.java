package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.petmeet.entity.OmsAfterSale;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.OmsAfterSaleMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.AdminAfterSaleService;
import org.petmeet.service.PmsProductService;
import org.petmeet.service.SysNotificationService;
import org.petmeet.vo.AdminAfterSaleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAfterSaleServiceImpl implements AdminAfterSaleService {

    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final SysUserMapper userMapper;
    private final PmsProductService productService;
    private final SysNotificationService notificationService;

    /**
     * 售后列表
     */
    @Override
    public Page<AdminAfterSaleVO> pageList(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<OmsAfterSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OmsAfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(OmsAfterSale::getAdminDeleted).or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE));
        if (status != null) {
            wrapper.eq(OmsAfterSale::getStatus, status);
        }

        if (StrUtil.isNotBlank(keyword)) {
            String k = keyword.trim();
            List<Long> orderIds = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                            .select(OmsOrder::getId)
                            .like(OmsOrder::getOrderSn, k))
                    .stream().map(OmsOrder::getId).toList();

            List<Long> itemIds = orderItemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>()
                            .select(OmsOrderItem::getId)
                            .like(OmsOrderItem::getProductName, k))
                    .stream().map(OmsOrderItem::getId).toList();

            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId)
                            .and(w -> w.like(SysUser::getUsername, k).or().like(SysUser::getNickname, k)))
                    .stream().map(SysUser::getId).toList();

            wrapper.and(w -> {
                boolean hasCondition = false;
                if (!orderIds.isEmpty()) {
                    w.in(OmsAfterSale::getOrderId, orderIds);
                    hasCondition = true;
                }
                if (!itemIds.isEmpty()) {
                    if (hasCondition) {
                        w.or();
                    }
                    w.in(OmsAfterSale::getOrderItemId, itemIds);
                    hasCondition = true;
                }
                if (!userIds.isEmpty()) {
                    if (hasCondition) {
                        w.or();
                    }
                    w.in(OmsAfterSale::getUserId, userIds);
                    hasCondition = true;
                }
                if (!hasCondition) {
                    w.eq(OmsAfterSale::getId, -1);
                }
            });
        }

        wrapper.orderByDesc(OmsAfterSale::getCreateTime);
        afterSaleMapper.selectPage(page, wrapper);

        List<OmsAfterSale> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }

        // 批量查询订单、商品项和用户信息
        Set<Long> orderIds = records.stream().map(OmsAfterSale::getOrderId).collect(Collectors.toSet());
        Set<Long> itemIds = records.stream().map(OmsAfterSale::getOrderItemId).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(OmsAfterSale::getUserId).collect(Collectors.toSet());

        Map<Long, OmsOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(OmsOrder::getId, o -> o, (a, b) -> a));

        Map<Long, OmsOrderItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : orderItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(OmsOrderItem::getId, i -> i, (a, b) -> a));

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 组装后台售后数据
        List<AdminAfterSaleVO> voList = new ArrayList<>();
        for (OmsAfterSale record : records) {
            AdminAfterSaleVO vo = new AdminAfterSaleVO();
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

            SysUser user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }

            voList.add(vo);
        }

        Page<AdminAfterSaleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 更新售后状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String remark) {
        // 查询售后申请
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getAdminDeleted())) {
            throw AppException.notFound("未找到售后请求");   }
        if (status == null || (status != OmsAfterSale.STATUS_PROCESSING
                && status != OmsAfterSale.STATUS_COMPLETED
                && status != OmsAfterSale.STATUS_REJECTED)) {
            throw AppException.badRequest("售后状态无效");   }
        Integer current = afterSale.getStatus();
        if (current != null && (current == OmsAfterSale.STATUS_COMPLETED
                || current == OmsAfterSale.STATUS_REJECTED
                || current == OmsAfterSale.STATUS_CANCELED)) {
            throw AppException.conflict("当前状态无法更改");   }
        String handleRemark = StrUtil.trimToNull(remark);
        LambdaUpdateWrapper<OmsAfterSale> updateWrapper = new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .and(w -> w.isNull(OmsAfterSale::getAdminDeleted)
                        .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE))
                .set(OmsAfterSale::getStatus, status)
                .set(OmsAfterSale::getHandleRemark, handleRemark)
                .set(OmsAfterSale::getHandleTime, LocalDateTime.now());
        if (current == null) {
            updateWrapper.isNull(OmsAfterSale::getStatus);
        } else {
            updateWrapper.eq(OmsAfterSale::getStatus, current);
        }
        int updated = afterSaleMapper.update(null, updateWrapper);
        if (updated <= 0) {
            throw AppException.conflict("售后状态已变化,请刷新后重试");
        }
        // 通知用户售后状态已变更
        OmsOrder order = orderMapper.selectById(afterSale.getOrderId());
        notificationService.sendToUser(
                afterSale.getUserId(),
                buildStatusNotificationTitle(status),
                buildStatusNotificationContent(status, order == null ? null :
                        order.getOrderSn(), handleRemark),
                "after_sale",
                afterSale.getId());
        if (order == null || order.getStatus() == null || order.getStatus()
                != OmsOrder.STATUS_REFUNDING) {
            return; }
        if (status == OmsAfterSale.STATUS_REJECTED) {
            // 驳回退款时恢复订单为已支付待发货
            boolean orderLocked = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                    .eq(OmsOrder::getId, order.getId())
                    .eq(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING)
                    .set(OmsOrder::getStatus, OmsOrder.STATUS_PAID)) > 0;
            if (!orderLocked) {
                throw AppException.conflict("订单状态已变化,请刷新后重试");
            }
            afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                    .eq(OmsAfterSale::getOrderId, order.getId())
                    .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING)
                    .set(OmsAfterSale::getStatus, OmsAfterSale.STATUS_REJECTED)
                    .set(OmsAfterSale::getHandleRemark, StrUtil.blankToDefault(handleRemark, "管理员已拒绝"))
                    .set(OmsAfterSale::getHandleTime, LocalDateTime.now()));
            return;  }
        if (status == OmsAfterSale.STATUS_COMPLETED) {
            // 所有售后都完成后，关闭订单并回滚库存
            Long activeCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<OmsAfterSale>()
                    .eq(OmsAfterSale::getOrderId, order.getId())
                    .and(w ->
                            w.isNull(OmsAfterSale::getAdminDeleted)
                                    .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE))
                    .in(OmsAfterSale::getStatus, OmsAfterSale.STATUS_PENDING, OmsAfterSale.STATUS_PROCESSING));
            if (activeCount != null && activeCount == 0) {
                boolean orderLocked = orderMapper.update(null, new LambdaUpdateWrapper<OmsOrder>()
                        .eq(OmsOrder::getId, order.getId())
                        .eq(OmsOrder::getStatus, OmsOrder.STATUS_REFUNDING)
                        .set(OmsOrder::getStatus, OmsOrder.STATUS_CLOSED)) > 0;
                if (!orderLocked) {
                    throw AppException.conflict("订单状态已变化,请刷新后重试");
                }
                rollbackStock(order.getId());
            }
        }
    }

    /**
     * 软删除售后
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long id) {
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || Integer.valueOf(OmsAfterSale.DELETE_DELETED).equals(afterSale.getAdminDeleted())) {
            throw AppException.notFound("售后请求不存在");
        }

        if (!canSoftDeleteStatus(afterSale.getStatus())) {
            throw AppException.badRequest("仅已完成、已拒绝或已取消的售后记录可删除");
        }

        afterSaleMapper.update(null, new LambdaUpdateWrapper<OmsAfterSale>()
                .eq(OmsAfterSale::getId, id)
                .and(w -> w.isNull(OmsAfterSale::getAdminDeleted)
                        .or().eq(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_VISIBLE))
                .set(OmsAfterSale::getAdminDeleted, OmsAfterSale.DELETE_DELETED));
    }

    /**
     * 批量软删除售后
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSoftDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        // 逐个复用单删逻辑
        for (Long id : normalizedIds) {
            softDelete(id);
        }
    }

    /**
     * 回滚库存
     */
    private void rollbackStock(Long orderId) {
        // 查询订单商品项
        List<OmsOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem item : items) {
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (item.getProductId() == null || qty <= 0) {
                continue;
            }
            // 把库存补回商品表
            productService.update(new LambdaUpdateWrapper<PmsProduct>()
                    .eq(PmsProduct::getId, item.getProductId())
                    .setSql("stock = stock + " + qty));
        }
    }

    private boolean canSoftDeleteStatus(Integer status) {
        return status != null && (status == OmsAfterSale.STATUS_COMPLETED
                || status == OmsAfterSale.STATUS_REJECTED
                || status == OmsAfterSale.STATUS_CANCELED);
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
            default -> "处理中";
        };
    }

    private String buildStatusNotificationTitle(Integer status) {
        if (status == null) {
            return "售后状态已更新";
        }
        return switch (status) {
            case OmsAfterSale.STATUS_PROCESSING -> "售后处理中";
            case OmsAfterSale.STATUS_COMPLETED -> "售后处理完成";
            case OmsAfterSale.STATUS_REJECTED -> "售后申请已驳回";
            default -> "售后状态已更新";
        };
    }

    private String buildStatusNotificationContent(Integer status, String orderSn, String remark) {
        String orderText = StrUtil.isBlank(orderSn) ? "你的订单" : ("订单" + orderSn);
        String safeRemark = StrUtil.trimToNull(remark);
        if (status == null) {
            return orderText + "售后状态已更新，请留意处理进度。";
        }
        return switch (status) {
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
