package com.nageoffer.shortlink.project.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkCreateRespDTO {


    /**
     * 原始链接
     */
    private String originUrl;

    private String fullShortUrl;
    private String gid;

}
