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

/**
 * (LinkAccessStatsDO)表实体类
 *
 * @author makejava
 * @since 2024-09-14 14:46:10
 */
@SuppressWarnings("serial")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_link_access_stats")
public class LinkAccessStatsDO extends BaseDO {
    //ID
    @TableId
    private Long id;
    //分组标识
    @TableField(exist = false)
    private String gid;
    //完整短链接
    private String fullShortUrl;
    //日期
    private Date date;
    //访问量
    private Integer pv;
    //独立访问数
    private Integer uv;
    //独立IP数
    private Integer uip;
    //小时
    private Integer hour;
    //星期
    private Integer weekday;
}
