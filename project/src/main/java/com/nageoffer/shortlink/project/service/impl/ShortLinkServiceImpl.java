package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValidDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper,ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;


    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;

    private final LinkStatsTodayMapper linkStatsTodayMapper;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String suffix = generateSuffix(requestParam);
        String fullShortUrl = createShortLinkDefaultDomain+"/"+suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDate(requestParam.getValidDate())
                .validDateType(requestParam.getValidDateType())
                .describe(requestParam.getDescribe())
                .fullShortUrl(fullShortUrl)
                .shortUri(suffix)
                .totalUip(0)
                .totalPv(0)
                .totalUv(0)
                .enableStatus(0)
                .build();
        ShortLinkGotoDo linkGotoDo = ShortLinkGotoDo.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(linkGotoDo);
        } catch (DuplicateKeyException ex) {
            log.warn("短链接：{} 重复入库",fullShortUrl);
            throw new ServiceException("短链接生成重复");
        }
        stringRedisTemplate.opsForValue()
                .set(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl),
                        requestParam.getOriginUrl(),
                        LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()),
                        TimeUnit.MILLISECONDS);

        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);

        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://"+shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {

        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
        return resultPage.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://"+result.getDomain());
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> shortLinkDOQueryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid,count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> maps = baseMapper.selectMaps(shortLinkDOQueryWrapper);
        return BeanUtil.copyToList(maps,ShortLinkGroupCountQueryRespDTO.class);
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValidDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(updateWrapper);
            baseMapper.insert(shortLinkDO);
        }
        //update短链接需要更新缓存
        // 如果有效期类型或者有效期发生了变化则需要删除缓存
        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            // 如果新连接没有过期，则需要删除原来的isnull的缓存
            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())) {
                if (Objects.equals(requestParam.getValidDateType(), ValidDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(new Date())) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName +serverPort+ "/"+shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(originalLink)){
            shortLinkStats(fullShortUrl,null,request,response);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        //布隆过滤器进行缓存穿透的控制
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if(!contains){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //布隆过滤器误判存在
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(gotoIsNullShortLink)){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if(StrUtil.isNotBlank(originalLink)){
                shortLinkStats(fullShortUrl,null,request,response);
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            LambdaQueryWrapper<ShortLinkGotoDo> eq = Wrappers.lambdaQuery(ShortLinkGotoDo.class)
                    .eq(ShortLinkGotoDo::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDo linkGotoDo = shortLinkGotoMapper.selectOne(eq);
            if(linkGotoDo==null){
                //进行风控
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl),"-",30,TimeUnit.DAYS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, linkGotoDo.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            //增加判空条件，使得移入回收站之后再恢复仍然可以正常跳转
            //如果getValidDate为空则代表永久有效
            if(shortLinkDO==null || (shortLinkDO.getValidDate()!=null&& shortLinkDO.getValidDate().before(new Date()))) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl),"-",30,TimeUnit.DAYS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            stringRedisTemplate.opsForValue()
                    .set(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl),
                            shortLinkDO.getOriginUrl(),
                            LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                            TimeUnit.MILLISECONDS);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
            shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response){
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        try {
            AtomicReference<String> uv = new AtomicReference<>();
            Runnable addResponseCookieTask = () -> {
                uv.set(UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60*60*24*30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf("/"),fullShortUrl.length()));
                ((HttpServletResponse) response).addCookie(uvCookie);
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl,uv.get());
                uvFirstFlag.set(Boolean.TRUE);
            };
            if(ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each-> Objects.equals(each.getName(),"uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            uv.set(each);
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                        },addResponseCookieTask);
            } else{
                addResponseCookieTask.run();
            }

            String remoteAddr = request.getRemoteAddr();
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDo> eq = Wrappers.lambdaQuery(ShortLinkGotoDo.class)
                        .eq(ShortLinkGotoDo::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDo linkGotoDo = shortLinkGotoMapper.selectOne(eq);
                gid = linkGotoDo.getGid();
            }
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekday = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(uvFirstFlag.get()? 1: 0)
                    .uip(uipFirstFlag? 1: 0)
                    .hour(hour)
                    .weekday(weekday)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", remoteAddr);
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infocode = localeResultObj.getString("infocode");
            LinkLocaleStatsDO linkLocaleStatsDO = new LinkLocaleStatsDO();

            if(StrUtil.isNotBlank(infocode) && StrUtil.equals(infocode,"10000")){
                String province = localeResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(unknownFlag? "未知":province)
                        .city(unknownFlag? "未知":localeResultObj.getString("city"))
                        .adcode(unknownFlag? "未知":localeResultObj.getString("adcode"))
                        .cnt(1)
                        .country("中国")
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO);
            }

            String os = LinkUtil.getOs(((HttpServletRequest) request));
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .os(os)
                    .date(new Date())
                    .build();
            linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);

            String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .browser(browser)
                    .cnt(1)
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);

            String device = LinkUtil.getDevice((HttpServletRequest) request);
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .device(device)
                    .cnt(1)
                    .build();
            linkDeviceStatsMapper.shortlinkDeviceStats(linkDeviceStatsDO);
            String network = LinkUtil.getNetwork((HttpServletRequest) request);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .network(network)
                    .cnt(1)
                    .build();
            linkNetworkStatsMapper.shortlinkNetworkStats(linkNetworkStatsDO);

            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(uv.get())
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .browser(browser)
                    .os(os)
                    .ip(remoteAddr)
                    .device(device)
                    .network(network)
                    .locale(StrUtil.join("-", "中国",linkLocaleStatsDO.getProvince(),linkLocaleStatsDO.getCity()))
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);
            baseMapper.incrementStats(gid,fullShortUrl,1,uvFirstFlag.get()? 1: 0,uipFirstFlag? 1: 0);
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .todayPv(1)
                    .todayUip(uipFirstFlag? 1: 0)
                    .todayUv(uvFirstFlag.get()? 1: 0)
                    .date(new Date())
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
        } catch (Throwable e) {
            log.error("短链接访问量统计异常",e);
        }
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int customGenerateCount = 0;
        String shortUri = null;
        while(true){
            if(customGenerateCount > 10){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)) {
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }
}
