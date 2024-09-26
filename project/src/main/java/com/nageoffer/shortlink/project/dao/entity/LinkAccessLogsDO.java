package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (LinkAccessLogs)表实体类
 *
 * @author makejava
 * @since 2024-09-18 17:18:28
 */
@SuppressWarnings("serial")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_link_access_logs")
public class LinkAccessLogsDO extends BaseDO {
    //ID
    @TableId
    private Long id;
    //完整短链接
    private String fullShortUrl;
    //分组标识
    private String gid;
    //用户信息
    private String user;
    //浏览器
    private String browser;
    //操作系统
    private String os;
    //IP
    private String ip;

    private String network;
    private String device;
    private String locale;
}
