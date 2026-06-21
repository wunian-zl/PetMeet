package org.petmeet.service.impl;

import org.petmeet.common.AppException;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.petmeet.dto.AddressDTO;
import org.petmeet.entity.UmsAddress;
import org.petmeet.mapper.UmsAddressMapper;
import org.petmeet.service.UmsAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UmsAddressServiceImpl extends ServiceImpl<UmsAddressMapper, UmsAddress> implements UmsAddressService {

    /**
     * 新增收货地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAddress(AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 把前端参数复制到地址对象
        UmsAddress address = new UmsAddress();
        BeanUtil.copyProperties(dto, address);
        address.setUserId(userId);

        // 如果本次设置为默认地址，先清空旧默认地址
        if (dto.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }

        this.save(address);
        return address.getId();
    }

    /**
     * 修改收货地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        UmsAddress existing = this.getById(dto.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw AppException.notFound("地址不存在");
        }

        // 新默认地址会覆盖旧默认地址
        if (dto.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }

        BeanUtil.copyProperties(dto, existing);
        this.updateById(existing);
    }

    /**
     * 删除收货地址
     */
    @Override
    public void deleteAddress(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        UmsAddress existing = this.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw AppException.notFound("地址不存在");
        }

        this.removeById(id);
    }

    /**
     * 当前用户地址列表
     */
    @Override
    public List<UmsAddress> listByCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<UmsAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsAddress::getUserId, userId)
                .orderByDesc(UmsAddress::getIsDefault);
        return this.list(wrapper);
    }

    /**
     * 默认收货地址
     */
    @Override
    public UmsAddress getDefaultAddress() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<UmsAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsAddress::getUserId, userId)
                .eq(UmsAddress::getIsDefault, 1);
        return this.getOne(wrapper);
    }

    /**
     * 清空默认地址
     */
    private void clearDefaultAddress(Long userId) {
        LambdaUpdateWrapper<UmsAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UmsAddress::getUserId, userId)
                .set(UmsAddress::getIsDefault, 0);
        this.update(wrapper);
    }
}
