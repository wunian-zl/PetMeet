package org.petmeet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petmeet.entity.CmsComplaint;
import org.petmeet.entity.CmsNote;
import org.petmeet.entity.OmsOrder;
import org.petmeet.entity.OmsOrderItem;
import org.petmeet.entity.PmsCategory;
import org.petmeet.entity.PmsProduct;
import org.petmeet.entity.SysUser;
import org.petmeet.dto.TopProductSalesDTO;
import org.petmeet.mapper.CmsComplaintMapper;
import org.petmeet.mapper.CmsNoteMapper;
import org.petmeet.mapper.OmsOrderMapper;
import org.petmeet.mapper.OmsOrderItemMapper;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.mapper.PmsProductMapper;
import org.petmeet.mapper.SysUserMapper;
import org.petmeet.service.AdminDashboardService;
import org.petmeet.vo.DashboardStatsVO;
import org.petmeet.vo.DashboardTrendVO;
import org.petmeet.vo.DashboardTopProductVO;
import org.petmeet.vo.DashboardTodoVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OmsOrderMapper omsOrderMapper;
    private final OmsOrderItemMapper omsOrderItemMapper;
    private final SysUserMapper sysUserMapper;
    private final CmsNoteMapper cmsNoteMapper;
    private final CmsComplaintMapper cmsComplaintMapper;
    private final PmsProductMapper pmsProductMapper;
    private final PmsCategoryMapper pmsCategoryMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String PV_KEY_PREFIX = "traffic:pv:";
    private static final DateTimeFormatter PV_DATE = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    /**
     * 看板统计数据
     */
    @Override
    public DashboardStatsVO getStats(String range) {
        DashboardStatsVO stats = new DashboardStatsVO();

        LocalDateTime startTime;
        LocalDateTime endTime = LocalDateTime.now();

        switch (range) {
            case "week" -> startTime = LocalDate.now().minusDays(7).atStartOfDay();
            case "month" -> startTime = LocalDate.now().minusDays(30).atStartOfDay();
            default -> startTime = LocalDate.now().atStartOfDay(); // 默认按今天统计
        }

        // 订单数
        Long orderCount = omsOrderMapper.selectCount(
                new LambdaQueryWrapper<OmsOrder>()
                        .ge(OmsOrder::getCreateTime, startTime)
                        .le(OmsOrder::getCreateTime, endTime));
        stats.setOrderCount(orderCount.intValue());

        // 销售额 - 简化处理,假设每单平均100元
        stats.setTotalSales(BigDecimal.valueOf(orderCount * 100));
        stats.setSalesChange(10.5); // 模拟环比

        // 新用户数
        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .ge(SysUser::getCreateTime, startTime)
                        .le(SysUser::getCreateTime, endTime));
        stats.setNewUserCount(userCount.intValue());
        stats.setUserChange(5.2);
        stats.setOrderChange(8.3);

        // 待审核内容数
        Long pendingNoteCount = cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, 0));
        stats.setPendingNoteCount(pendingNoteCount.intValue());

        // 待发货订单数
        Long pendingShipCount = omsOrderMapper.selectCount(
                new LambdaQueryWrapper<OmsOrder>().eq(OmsOrder::getStatus, 1));
        stats.setPendingShipCount(pendingShipCount.intValue());

        return stats;
    }

    /**
     * 看板趋势数据
     */
    @Override
    public DashboardTrendVO getTrend(String range) {
        DashboardTrendVO trend = new DashboardTrendVO();

        int days = "month".equals(range) ? 30 : 7;
        List<String> labels = new ArrayList<>();
        List<Integer> pageViews = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            labels.add(date.format(formatter));

            List<OmsOrder> dayOrders = omsOrderMapper.selectList(
                    new LambdaQueryWrapper<OmsOrder>()
                            .ge(OmsOrder::getCreateTime, date.atStartOfDay())
                            .lt(OmsOrder::getCreateTime, date.plusDays(1).atStartOfDay()));
            int dayOrderCount = dayOrders.size();
            BigDecimal daySales = dayOrders.stream()
                    .map(OmsOrder::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            orderCounts.add(dayOrderCount);
            pageViews.add(getPv(date));
            salesData.add(daySales.doubleValue());
        }

        trend.setLabels(labels);
        trend.setPageViews(pageViews);
        trend.setOrderCounts(orderCounts);
        trend.setSalesData(salesData);

        return trend;
    }

    /**
     * 读取访问量
     */
    private int getPv(LocalDate date) {
        if (date == null) {
            return 0;
        }
        try {
            String v = redisTemplate.opsForValue().get(PV_KEY_PREFIX + date.format(PV_DATE));
            if (v == null || v.isBlank()) {
                return 0;
            }
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 分类销售额
     */
    @Override
    public List<Map<String, Object>> getCategorySales() {
        // 查询所有订单商品项
        List<OmsOrderItem> items = omsOrderItemMapper.selectList(new LambdaQueryWrapper<>());
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> productIds = items.stream()
                .map(OmsOrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, PmsProduct> productMap = pmsProductMapper.selectBatchIds(productIds).stream()
                .collect(HashMap::new, (map, product) -> map.put(product.getId(), product), HashMap::putAll);

        // 按分类累计销售额
        Map<Long, BigDecimal> categoryTotals = new HashMap<>();
        for (OmsOrderItem item : items) {
            PmsProduct product = productMap.get(item.getProductId());
            if (product == null || product.getCategoryId() == null) {
                continue;
            }
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity());
            BigDecimal amount = price.multiply(qty);
            categoryTotals.merge(product.getCategoryId(), amount, BigDecimal::add);
        }

        if (categoryTotals.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> categoryNameMap = pmsCategoryMapper.selectBatchIds(categoryTotals.keySet()).stream()
                .collect(HashMap::new, (map, c) -> map.put(c.getId(), c.getName()), HashMap::putAll);

        // 组装图表需要的数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categoryTotals.entrySet()) {
            String name = categoryNameMap.getOrDefault(entry.getKey(), "其他");
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", entry.getValue().doubleValue());
            result.add(item);
        }
        return result;
    }

    /**
     * 销量排行
     */
    @Override
    public List<DashboardTopProductVO> getTopProducts() {
        // 从已支付订单明细实时汇总销量TOP5（避免依赖 pms_product.sales 是否已同步）
        List<TopProductSalesDTO> topSales = omsOrderItemMapper.selectTopPaidProductSales(5);
        if (topSales == null || topSales.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> productIds = topSales.stream()
                .map(TopProductSalesDTO::getProductId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, PmsProduct> productMap = pmsProductMapper.selectBatchIds(productIds).stream()
                .collect(HashMap::new, (map, product) -> map.put(product.getId(), product), HashMap::putAll);
        if (productMap.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = productMap.values().stream()
                .map(PmsProduct::getCategoryId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : pmsCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(HashMap::new, (map, c) -> map.put(c.getId(), c.getName()), HashMap::putAll);

        List<DashboardTopProductVO> result = new ArrayList<>();
        for (TopProductSalesDTO row : topSales) {
            if (row == null || row.getProductId() == null) {
                continue;
            }
            PmsProduct p = productMap.get(row.getProductId());
            if (p == null) {
                continue;
            }
            DashboardTopProductVO vo = new DashboardTopProductVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setCover(p.getCoverImg());
            vo.setCategoryId(p.getCategoryId());

            int sales = row.getSales() == null ? 0 : Math.toIntExact(row.getSales());
            BigDecimal price = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();

            vo.setSales(sales);
            vo.setStock(p.getStock() == null ? 0 : p.getStock());
            vo.setCategoryName(categoryNameMap.getOrDefault(p.getCategoryId(), "未分类"));
            vo.setAmount(price.multiply(BigDecimal.valueOf(sales)));
            result.add(vo);
        }
        return result;
    }

    /**
     * 待办事项
     */
    @Override
    public List<DashboardTodoVO> getTodos() {
        List<DashboardTodoVO> todos = new ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 待审核内容
        Long pendingNotes = cmsNoteMapper.selectCount(
                new LambdaQueryWrapper<CmsNote>().eq(CmsNote::getStatus, 0));
        if (pendingNotes > 0) {
            DashboardTodoVO todo = new DashboardTodoVO();
            todo.setId(1L);
            todo.setType("note");
            todo.setTitle("待审核内容");
            todo.setDescription("有 " + pendingNotes + " 条内容等待审核");
            todo.setLink("/admin/content");
            todo.setPriority("high");
            todos.add(todo);
        }

        // 待发货订单
        Long pendingOrders = omsOrderMapper.selectCount(
                new LambdaQueryWrapper<OmsOrder>().eq(OmsOrder::getStatus, 1));
        if (pendingOrders > 0) {
            DashboardTodoVO todo = new DashboardTodoVO();
            todo.setId(2L);
            todo.setType("order");
            todo.setTitle("待发货订单");
            todo.setDescription("有 " + pendingOrders + " 个订单等待发货");
            todo.setLink("/admin/order");
            todo.setPriority("high");
            todos.add(todo);
        }

        // 待处理投诉
        Long pendingComplaints = cmsComplaintMapper.selectCount(
                new LambdaQueryWrapper<CmsComplaint>().eq(CmsComplaint::getStatus, 0));
        if (pendingComplaints > 0) {
            DashboardTodoVO todo = new DashboardTodoVO();
            todo.setId(4L);
            todo.setType("complaint");
            todo.setTitle("待处理投诉");
            todo.setDescription("有 " + pendingComplaints + " 条投诉待处理");
            todo.setLink("/admin/complaint");
            todo.setPriority("high");
            todos.add(todo);
        }

        // 用户不满意反馈（今日）
        Long unsatisfiedFeedbackToday = cmsComplaintMapper.selectCount(
                new LambdaQueryWrapper<CmsComplaint>()
                        .eq(CmsComplaint::getFeedbackStatus, 2)
                        .ge(CmsComplaint::getFeedbackTime, todayStart)
                        .ne(CmsComplaint::getStatus, 0)
        // 排除已经发起再次投诉的记录（parent_id 指向当前投诉）。
                        .apply("NOT EXISTS (SELECT 1 FROM cms_complaint c2 WHERE c2.parent_id = cms_complaint.id)")
        );
        if (unsatisfiedFeedbackToday > 0) {
            DashboardTodoVO todo = new DashboardTodoVO();
            todo.setId(5L);
            todo.setType("complaint");
            todo.setTitle("用户不满意反馈");
            todo.setDescription("今日有 " + unsatisfiedFeedbackToday + " 条投诉被用户标记为不满意");
            todo.setLink("/admin/complaint");
            todo.setPriority("medium");
            todos.add(todo);
        }

        // 低库存商品
        Long lowStockProducts = pmsProductMapper.selectCount(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getStatus, 1)
                        .and(w -> w
                                .nested(w1 -> w1.isNull(PmsProduct::getWarningStock).le(PmsProduct::getStock, 10))
                                .or(w1 -> w1.isNotNull(PmsProduct::getWarningStock).apply("stock <= warning_stock"))
                        ));
        if (lowStockProducts > 0) {
            DashboardTodoVO todo = new DashboardTodoVO();
            todo.setId(3L);
            todo.setType("product");
            todo.setTitle("库存预警");
            todo.setDescription("有 " + lowStockProducts + " 个商品库存不足");
            todo.setLink("/admin/product");
            todo.setPriority("medium");
            todos.add(todo);
        }

        return todos;
    }
}
