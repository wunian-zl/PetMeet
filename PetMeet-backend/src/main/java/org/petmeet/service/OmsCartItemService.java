package org.petmeet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.petmeet.dto.CartAddDTO;
import org.petmeet.entity.OmsCartItem;
import org.petmeet.vo.CartItemVO;
import java.util.List;

public interface OmsCartItemService extends IService<OmsCartItem> {
    void addToCart(CartAddDTO dto);

    void updateQuantity(Long cartItemId, Integer quantity);

    void deleteCartItem(Long cartItemId);

    void removeFromCart(Long cartItemId);

    void batchDelete(List<Long> cartItemIds);

    List<CartItemVO> listByCurrentUser();

    List<CartItemVO> getCartList();

    void clearCart();

    void updateSelected(Long cartItemId, Boolean selected);

    void selectAll(Boolean selected);

    Integer getCartCount();
}
