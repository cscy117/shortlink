package com.nageoffer.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespRTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkController {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return shortLinkRemoteService.createShortLink(requestParam);
    }
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespRTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkRemoteService.pageShortLink(requestParam);
    }
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        return shortLinkRemoteService.updateShortLink(requestParam);
    }
}
