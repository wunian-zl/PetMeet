package org.petmeet.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petmeet.common.Result;
import org.petmeet.dto.PayCreateDTO;
import org.petmeet.service.PayService;
import org.petmeet.vo.PayResponseVO;
import org.petmeet.vo.PayStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
@Tag(name = "Pay APIs", description = "Create payment, query payment status and receive channel callbacks")
public class PayController {

    private final PayService payService;

    @SaCheckLogin
    @PostMapping("/create")
    @Operation(summary = "Create pay order")
    public Result<PayResponseVO> createPay(@Valid @RequestBody PayCreateDTO dto) {
        return Result.success(payService.createPay(dto));
    }

    @SaCheckLogin
    @GetMapping("/status/{paySn}")
    @Operation(summary = "Query pay status")
    public Result<PayStatusVO> queryPayStatus(
            @PathVariable String paySn,
            @RequestParam(defaultValue = "false") Boolean syncChannel) {
        return Result.success(payService.queryPayStatus(paySn, Boolean.TRUE.equals(syncChannel)));
    }

    @SaCheckLogin
    @PostMapping("/mock/confirm/{paySn}")
    @Operation(summary = "Confirm mock payment")
    public Result<Void> mockConfirm(@PathVariable String paySn) {
        payService.mockConfirm(paySn);
        return Result.success("支付成功", null);
    }

    @PostMapping("/alipay/notify")
    @Operation(summary = "Alipay async notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        return payService.handleAlipayNotify(params);
    }
}
