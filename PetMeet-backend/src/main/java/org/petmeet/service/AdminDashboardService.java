package org.petmeet.service;

import org.petmeet.vo.DashboardStatsVO;
import org.petmeet.vo.DashboardTrendVO;
import org.petmeet.vo.DashboardTopProductVO;
import org.petmeet.vo.DashboardTodoVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端看板服务接口
 */
public interface AdminDashboardService {

    DashboardStatsVO getStats(String range);

    DashboardTrendVO getTrend(String range);

    List<Map<String, Object>> getCategorySales();

    List<DashboardTopProductVO> getTopProducts();

    List<DashboardTodoVO> getTodos();
}
