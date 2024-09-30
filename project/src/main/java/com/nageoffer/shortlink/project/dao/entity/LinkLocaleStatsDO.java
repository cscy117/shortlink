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
 * (TLinkLocaleStats)表实体类
 *
 * @author makejava
 * @since 2024-09-18 15:13:48
 */
@SuppressWarnings("serial")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_link_locale_stats")
public class LinkLocaleStatsDO extends BaseDO {
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
    //省份名称
    private String province;
    //市名称
    private String city;
    //城市编码
    private String adcode;
    //国家标识
    private String country;

}
