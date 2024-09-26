package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkNetworkStatsDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {
    @Insert("INSERT INTO t_link_network_stats (full_short_url,gid,date,cnt,network,create_time,update_time,del_flag)\n" +
            "VALUES (#{linkNetworkStats.fullShortUrl},#{linkNetworkStats.gid},#{linkNetworkStats.date},#{linkNetworkStats.cnt},#{linkNetworkStats.network},NOW(),NOW(),0) \n" +
            "ON DUPLICATE KEY UPDATE cnt = cnt + #{linkNetworkStats.cnt},create_time=create_time,update_time=NOW();")
    void shortlinkNetworkStats(@Param("linkNetworkStats") LinkNetworkStatsDO linkNetworkStatsDO);

    /**
     * 根据短链接获取指定日期内访问网络监控数据
     */
    @Select("SELECT " +
            "    network, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_network_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, network;")
    List<LinkNetworkStatsDO> listNetworkStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

}
