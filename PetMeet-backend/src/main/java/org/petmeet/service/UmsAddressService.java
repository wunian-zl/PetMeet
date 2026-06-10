package org.petmeet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.AddressDTO;
import org.petmeet.entity.UmsAddress;
import java.util.List;

public interface UmsAddressService extends IService<UmsAddress> {
    Long saveAddress(AddressDTO dto);

    void updateAddress(AddressDTO dto);

    void deleteAddress(Long id);

    List<UmsAddress> listByCurrentUser();

    UmsAddress getDefaultAddress();
}
