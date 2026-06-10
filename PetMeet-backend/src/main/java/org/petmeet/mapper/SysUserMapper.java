package org.petmeet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.petmeet.entity.SysUser;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
