package org.petmeet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.petmeet.entity.PmsProduct;

@Mapper
public interface PmsProductMapper extends BaseMapper<PmsProduct> {
}
