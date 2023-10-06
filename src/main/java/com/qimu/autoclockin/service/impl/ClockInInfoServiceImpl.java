package com.qimu.autoclockin.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.mapper.ClockInInfoMapper;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.vo.Detail;
import com.qimu.autoclockin.service.ClockInInfoService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.qimu.autoclockin.constant.RequestAddressConstant.LONGITUDE_AND_LATITUDE;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:45:17
 * @Version: 1.0
 * @Description: 打卡信息服务impl
 */
@Service
public class ClockInInfoServiceImpl extends ServiceImpl<ClockInInfoMapper, ClockInInfo>
        implements ClockInInfoService {
    @Override
    public void validClockInInfo(ClockInInfo clockInInfo, boolean add) {
        if (clockInInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String address = clockInInfo.getAddress();
        String deviceType = clockInInfo.getDeviceType();
        String deviceId = clockInInfo.getDeviceId();
        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(address, deviceId, deviceType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        Detail detail = JSONUtil.toBean(HttpUtil.get(LONGITUDE_AND_LATITUDE + address), Detail.class);
        boolean detailStatus = ObjectUtils.isEmpty(detail) || StringUtils.isAnyBlank(detail.getDetail().getPointx(), detail.getDetail().getPointy());
        if (detailStatus) {
            if (StringUtils.isAnyBlank(clockInInfo.getLatitude(), clockInInfo.getLongitude())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "经纬度获取异常");
            }
            clockInInfo.setLatitude(clockInInfo.getLatitude());
            clockInInfo.setLongitude(clockInInfo.getLongitude());
        } else {
            clockInInfo.setLatitude(detail.getDetail().getPointy());
            clockInInfo.setLongitude(detail.getDetail().getPointx());
        }
    }
}




