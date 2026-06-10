package org.petmeet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.petmeet.entity.PmsCategory;
import org.petmeet.mapper.PmsCategoryMapper;
import org.petmeet.service.PmsCategoryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PmsCategoryServiceImpl extends ServiceImpl<PmsCategoryMapper, PmsCategory> implements PmsCategoryService {

    /**
     * 启用分类列表
     */
    @Override
    public List<PmsCategory> listEnabled() {
        LambdaQueryWrapper<PmsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PmsCategory::getStatus, 1).orderByAsc(PmsCategory::getSort);
        return this.list(wrapper);
    }
}
