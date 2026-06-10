package org.petmeet.service;

public interface FollowService {
    boolean toggleFollow(Long followeeId);

    boolean isFollowing(Long followeeId);

    int countFollowers(Long userId);

    int countFollowing(Long userId);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.petmeet.vo.FollowUserVO> pageFollowers(Long userId, Integer pageNum, Integer pageSize);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.petmeet.vo.FollowUserVO> pageFollowing(Long userId, Integer pageNum, Integer pageSize);
}
