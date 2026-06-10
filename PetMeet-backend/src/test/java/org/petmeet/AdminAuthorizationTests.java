package org.petmeet;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petmeet.entity.SysUser;
import org.petmeet.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AdminAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

    private final List<Long> loginIds = new ArrayList<>();

    @AfterEach
    void clearLoginState() {
        loginIds.forEach(StpUtil::kickout);
    }

    @Test
    void anonymousUserCannotAccessAdminApi() throws Exception {
        mockMvc.perform(post("/admin/auth/logout"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void normalUserCannotAccessAdminApi() throws Exception {
        SysUser user = createUser("user");
        String token = login(user.getId());

        mockMvc.perform(post("/admin/auth/logout")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanAccessAdminApi() throws Exception {
        SysUser admin = createUser("admin");
        String token = login(admin.getId());

        mockMvc.perform(post("/admin/auth/logout")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void anonymousUserCannotAccessCategoryManagement() throws Exception {
        mockMvc.perform(get("/category/list/all"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void normalUserCannotAccessCategoryManagement() throws Exception {
        SysUser user = createUser("user");
        String token = login(user.getId());

        mockMvc.perform(get("/category/list/all")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanAccessCategoryManagement() throws Exception {
        SysUser admin = createUser("admin");
        String token = login(admin.getId());

        mockMvc.perform(get("/category/list/all")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void anonymousUserCannotUpload() throws Exception {
        mockMvc.perform(multipart("/common/upload/image")
                        .file(new MockMultipartFile(
                                "file", "test.png", "image/png",
                                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47}))
                        .param("biz", "noteImage"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void normalUserCannotUploadAdminAsset() throws Exception {
        SysUser user = createUser("user");
        String token = login(user.getId());

        mockMvc.perform(multipart("/common/upload/image")
                        .file(new MockMultipartFile(
                                "file", "test.png", "image/png",
                                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47}))
                        .param("biz", "productCover")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void loggedInUserCannotUploadFileWithForgedImageExtension() throws Exception {
        SysUser user = createUser("user");
        String token = login(user.getId());

        mockMvc.perform(multipart("/common/upload/image")
                        .file(new MockMultipartFile(
                                "file", "fake.png", "image/png",
                                "not-an-image".getBytes()))
                        .param("biz", "noteImage")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.msg").value("文件内容与扩展名不匹配"));
    }

    private SysUser createUser(String role) {
        SysUser user = new SysUser();
        user.setUsername("auth_test_" + role + "_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword("test-only");
        user.setNickname("authorization test");
        user.setRole(role);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private String login(Long userId) {
        StpUtil.login(userId);
        loginIds.add(userId);
        return StpUtil.getTokenValue();
    }
}
