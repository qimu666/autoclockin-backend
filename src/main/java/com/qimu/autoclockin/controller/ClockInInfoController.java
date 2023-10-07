package com.qimu.autoclockin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qimu.autoclockin.annotation.AuthCheck;
import com.qimu.autoclockin.common.BaseResponse;
import com.qimu.autoclockin.common.DeleteRequest;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.common.ResultUtils;
import com.qimu.autoclockin.constant.CommonConstant;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoAddRequest;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoQueryRequest;
import com.qimu.autoclockin.model.dto.clockInInfo.ClockInInfoUpdateRequest;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.model.vo.UserVO;
import com.qimu.autoclockin.service.ClockInInfoService;
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
import static com.qimu.autoclockin.job.ClockInJob.getObtainClockInTime;

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
    private ClockInInfoService clockInInfoService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private UserService userService;

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
        clockInInfo.setUserId(loginUser.getId());
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
        boolean result = clockInInfoService.updateById(clockInInfo);
        if (oldClockInInfo.getStatus().equals(ClockInStatusEnum.STARTING.getValue())) {
            long secondsUntilUserTime = getObtainClockInTime(user.getId(), clockInInfoUpdateRequest.getClockInTime());
            if (secondsUntilUserTime > 0) {
                redisTemplate.opsForValue().set(SIGN_USER_GROUP + oldClockInInfo.getUserId(), String.valueOf(oldClockInInfo.getUserId()), secondsUntilUserTime, TimeUnit.SECONDS);
            }
        }
        return ResultUtils.success(result);
    }

    /**
     * 通过id获取打卡信息
     *
     * @param id id
     * @return {@link BaseResponse}<{@link ClockInInfo}>
     */
    @GetMapping("/get")
    public BaseResponse<ClockInInfo> getClockInInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ClockInInfo clockInInfo = clockInInfoService.getById(id);
        return ResultUtils.success(clockInInfo);
    }


    /**
     * 让时钟信息登录用户id
     * 根据 id 获取
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link ClockInInfo}>
     */
    @GetMapping("/login/get")
    public BaseResponse<ClockInInfo> getClockInInfoByLoginUserId(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, loginUser.getId());
        ClockInInfo clockInInfoServiceOne = clockInInfoService.getOne(clockInInfoQueryWrapper);
        return ResultUtils.success(clockInInfoServiceOne);
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

        Integer status = clockInInfoQueryRequest.getStatus();

        // content 需支持模糊搜索
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<ClockInInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(address), "address", address).like(StringUtils.isNotBlank(deviceType), "deviceType", deviceType).eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<ClockInInfo> clockInInfoPage = clockInInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(clockInInfoPage);
    }
    // endregion
}
