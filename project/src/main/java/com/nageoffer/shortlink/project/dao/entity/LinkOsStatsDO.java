package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@SuppressWarnings("serial")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_link_os_stats")
public class LinkOsStatsDO extends BaseDO {
    //ID
    @TableId
    private Long id;
    //完整短链接
    private String fullShortUrl;
    //分组标识
    @TableField(exist = false)
    private String gid;
    //日期
    private Date date;
    //访问量
    private Integer cnt;
    //操作系统
    private String os;

}
