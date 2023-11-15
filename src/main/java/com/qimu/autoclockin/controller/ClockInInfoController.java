package com.qimu.autoclockin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qimu.autoclockin.annotation.AuthCheck;
import com.qimu.autoclockin.common.BaseResponse;
import com.qimu.autoclockin.common.DeleteRequest;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.common.ResultUtils;
import com.qimu.autoclockin.constant.CommonConstant;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.autoclockin.AutoClockInClient;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoAddRequest;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoQueryRequest;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoUpdateRequest;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.model.vo.ClockInInfoVo;
import com.qimu.autoclockin.model.vo.LoginResult;
import com.qimu.autoclockin.model.vo.LoginResultVO;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.ClockInConstant.SIGN_USER_GROUP;
import static com.qimu.autoclockin.constant.UserConstant.ADMIN_ROLE;
import static com.qimu.autoclockin.job.ClockInJob.getObtainClockInTime;
import static com.qimu.autoclockin.utils.AutoSignUtils.login;

/**
 * 打卡信息接口
 *
 * @author qimu
 */
@RestController
@RequestMapping("/clockInInfo")
@Slf4j
public class ClockInInfoController {
    @Resource
    private AutoClockInClient autoClockInClient;
    @Resource
    private ClockInInfoService clockInInfoService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private UserService userService;
    @Resource
    private DailyCheckInService dailyCheckInService;

    // region 增删改查

    /**
     * 添加打卡信息
     *
     * @param clockInInfoAddRequest 打卡信息添加请求
     * @param request               要求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/add")
    public BaseResponse<Long> addClockInInfo(@RequestBody ClockInInfoAddRequest clockInInfoAddRequest, HttpServletRequest request) {
        if (clockInInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfo = new ClockInInfo();
        BeanUtils.copyProperties(clockInInfoAddRequest, clockInInfo);
        // 校验
        clockInInfoService.validClockInInfo(clockInInfo, true);
        User loginUser = userService.getLoginUser(request);

        LambdaQueryWrapper<ClockInInfo> clockInInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoLambdaQueryWrapper.eq(ClockInInfo::getClockInAccount, clockInInfoAddRequest.getClockInAccount());
        long count = clockInInfoService.count(clockInInfoLambdaQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该账号打卡信息已存在");
        }

        if (loginUser.getUserRole().equals(ADMIN_ROLE) && StringUtils.isNotBlank(clockInInfoAddRequest.getUserAccount())) {
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getUserAccount, clockInInfoAddRequest.getUserAccount());
            User user = userService.getOne(userLambdaQueryWrapper);
            if (user == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "平台账号不存在");
            }
            clockInInfo.setUserId(user.getId());
            if (StringUtils.isNotBlank(clockInInfoAddRequest.getEmail())) {
                user.setEmail(clockInInfoAddRequest.getEmail());
                userService.updateById(user);
            }
        } else {
            clockInInfo.setUserId(loginUser.getId());
        }
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        BeanUtils.copyProperties(clockInInfoAddRequest, clockInInfoVo);
        try {
            LoginResultVO loginResultVO = login(clockInInfoVo, autoClockInClient);
            LoginResult loginResult = loginResultVO.getLoginResult();
            if (ObjectUtils.anyNull(loginResult, loginResultVO)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
            if (ObjectUtils.isNotEmpty(loginResult) && loginResult.getCode() != 1001) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
        boolean result = clockInInfoService.save(clockInInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newClockInInfoId = clockInInfo.getId();
        return ResultUtils.success(newClockInInfoId);
    }

    /**
     * 删除打卡信息
     *
     * @param deleteRequest 删除请求
     * @param request       要求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteClockInInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ClockInInfo oldClockInInfo = clockInInfoService.getById(id);
        if (oldClockInInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldClockInInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = clockInInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新打卡信息
     *
     * @param clockInInfoUpdateRequest 打卡信息更新请求
     * @param request                  要求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateClockInInfo(@RequestBody ClockInInfoUpdateRequest clockInInfoUpdateRequest, HttpServletRequest request) {
        if (ObjectUtils.anyNull(clockInInfoUpdateRequest, clockInInfoUpdateRequest.getId()) || clockInInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfo = new ClockInInfo();
        BeanUtils.copyProperties(clockInInfoUpdateRequest, clockInInfo);
        // 参数校验
        clockInInfoService.validClockInInfo(clockInInfo, false);
        User user = userService.getLoginUser(request);
        long id = clockInInfoUpdateRequest.getId();
        // 判断是否存在
        ClockInInfo oldClockInInfo = clockInInfoService.getById(id);
        if (oldClockInInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!userService.isAdmin(request) && !oldClockInInfo.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        BeanUtils.copyProperties(clockInInfoUpdateRequest, clockInInfoVo);
        try {
            LoginResultVO loginResultVO = login(clockInInfoVo, autoClockInClient);
            LoginResult loginResult = loginResultVO.getLoginResult();
            if (ObjectUtils.anyNull(loginResult, loginResultVO)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
            if (ObjectUtils.isNotEmpty(loginResult) && loginResult.getCode() != 1001) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
        boolean result = clockInInfoService.updateById(clockInInfo);
        if (oldClockInInfo.getStatus().equals(ClockInStatusEnum.STARTING.getValue())) {
            long secondsUntilUserTime = getObtainClockInTime(clockInInfo);
            if (secondsUntilUserTime > 0) {
                redisTemplate.opsForValue().set(SIGN_USER_GROUP + oldClockInInfo.getClockInAccount(), String.valueOf(oldClockInInfo.getClockInAccount()), secondsUntilUserTime, TimeUnit.SECONDS);
            }
        }
        return ResultUtils.success(result);
    }


    /**
     * 通过id获取打卡信息
     *
     * @param id id
     * @return {@link BaseResponse}<{@link ClockInInfoVo}>
     */
    @GetMapping("/get")
    public BaseResponse<ClockInInfoVo> getClockInInfoById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfo = clockInInfoService.getById(id);
        return ResultUtils.success(getClockInInfoVo(clockInInfo));
    }


    /**
     * 通过登录用户id获取打卡信息
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link ClockInInfoVo}>
     */
    @GetMapping("/login/get")
    public BaseResponse<ClockInInfoVo> getClockInInfoByLoginUserId(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, loginUser.getId());
        List<ClockInInfo> clockInInfos = clockInInfoService.list(clockInInfoQueryWrapper);
        ClockInInfoVo clockInInfoVo = null;
        if (CollectionUtils.isNotEmpty(clockInInfos)) {
            clockInInfoVo = getClockInInfoVo(clockInInfos.get(0));
        }
        return ResultUtils.success(clockInInfoVo);
    }

    private ClockInInfoVo getClockInInfoVo(ClockInInfo clockInInfo) {
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        if (clockInInfo != null) {
            BeanUtils.copyProperties(clockInInfo, clockInInfoVo);
            LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getClockInAccount, clockInInfo.getClockInAccount());
            DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
            if (checkInServiceOne != null) {
                clockInInfoVo.setDescription(checkInServiceOne.getDescription());
            }
            return clockInInfoVo;
        }
        return null;
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param clockInInfoQueryRequest 打卡信息查询请求
     * @return {@link BaseResponse}<{@link List}<{@link ClockInInfo}>>
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<ClockInInfo>> listClockInInfo(ClockInInfoQueryRequest clockInInfoQueryRequest) {
        ClockInInfo clockInInfoQuery = new ClockInInfo();
        if (clockInInfoQueryRequest != null) {
            BeanUtils.copyProperties(clockInInfoQueryRequest, clockInInfoQuery);
        }
        QueryWrapper<ClockInInfo> queryWrapper = new QueryWrapper<>(clockInInfoQuery);
        List<ClockInInfo> clockInInfoList = clockInInfoService.list(queryWrapper);
        return ResultUtils.success(clockInInfoList);
    }

    /**
     * 分页获取列表
     *
     * @param clockInInfoQueryRequest 打卡信息查询请求
     * @param request                 要求
     * @return {@link BaseResponse}<{@link Page}<{@link ClockInInfo}>>
     */
    @GetMapping("/list/page")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<ClockInInfo>> listClockInInfoByPage(ClockInInfoQueryRequest clockInInfoQueryRequest, HttpServletRequest request) {
        if (clockInInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfoQuery = new ClockInInfo();
        BeanUtils.copyProperties(clockInInfoQueryRequest, clockInInfoQuery);
        long current = clockInInfoQueryRequest.getCurrent();
        long size = clockInInfoQueryRequest.getPageSize();
        String sortField = clockInInfoQueryRequest.getSortField();
        String sortOrder = clockInInfoQueryRequest.getSortOrder();

        String address = clockInInfoQueryRequest.getAddress();
        String deviceType = clockInInfoQueryRequest.getDeviceType();
        String clockInAccount = clockInInfoQueryRequest.getClockInAccount();

        Integer status = clockInInfoQueryRequest.getStatus();

        // content 需支持模糊搜索
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<ClockInInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(address), "address", address)
                .like(StringUtils.isNotBlank(deviceType), "deviceType", deviceType)
                .eq(ObjectUtils.isNotEmpty(status), "status", status)
                .eq(ObjectUtils.isNotEmpty(clockInAccount), "clockInAccount", clockInAccount);

        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ClockInInfo> clockInInfoPage = clockInInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(clockInInfoPage);
    }

    /**
     * 按页面列出我添加的打卡信息
     *
     * @param clockInInfoQueryRequest 打卡信息查询请求
     * @param request                 要求
     * @return {@link BaseResponse}<{@link Page}<{@link ClockInInfo}>>
     */
    @GetMapping("/list/myClockInInfo")
    public BaseResponse<Page<ClockInInfo>> listMyClockInInfoByPage(ClockInInfoQueryRequest clockInInfoQueryRequest, HttpServletRequest request) {
        if (clockInInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfoQuery = new ClockInInfo();
        BeanUtils.copyProperties(clockInInfoQueryRequest, clockInInfoQuery);
        long current = clockInInfoQueryRequest.getCurrent();
        long size = clockInInfoQueryRequest.getPageSize();
        String sortField = clockInInfoQueryRequest.getSortField();
        String sortOrder = clockInInfoQueryRequest.getSortOrder();

        String address = clockInInfoQueryRequest.getAddress();
        String deviceType = clockInInfoQueryRequest.getDeviceType();
        String clockInAccount = clockInInfoQueryRequest.getClockInAccount();
        Integer status = clockInInfoQueryRequest.getStatus();
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // content 需支持模糊搜索
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<ClockInInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(address), "address", address)
                .like(StringUtils.isNotBlank(deviceType), "deviceType", deviceType)
                .eq(ObjectUtils.isNotEmpty(status), "status", status)
                .eq(ObjectUtils.isNotEmpty(loginUser.getId()), "userId", loginUser.getId())
                .eq(ObjectUtils.isNotEmpty(clockInAccount), "clockInAccount", clockInAccount);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ClockInInfo> clockInInfoPage = clockInInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(clockInInfoPage);
    }
    // endregion
}
