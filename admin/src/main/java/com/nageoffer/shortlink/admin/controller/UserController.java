package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUserName(@PathVariable("username") String username){
        UserRespDTO result = userService.getUserByUsername(username);
        //return new Result<UserRespDTO>().setCode("0").setData(result);
        if(result==null){
            return new Result<UserRespDTO>().setCode(UserErrorCodeEnum.USER_NULL.code()).setMessage(UserErrorCodeEnum.USER_NULL.message());
            //return Results.failure(UserErrorCodeEnum.USER_NULL.code());
        }
        else{
            return Results.success(result);
            //return new Result<UserRespDTO>().setCode("0").setData(result);
        }
    }
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUserByUsername(@RequestParam("username") String username){
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 注册用户
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam){
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     * @param requestParam
     * @return
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam){
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam){
        return Results.success(userService.login(requestParam));
    }

    @GetMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username,@RequestParam("token") String token){
        return Results.success(userService.checkLogin(username,token));
    }

    /**
     * 用户退出登录
     * @param username
     * @param token
     * @return
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username,@RequestParam("token") String token){
        userService.logout(username,token);
        return Results.success();
    }
}
