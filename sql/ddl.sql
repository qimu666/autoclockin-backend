-- 创建库
create database if not exists autoclockin;

-- 切换库
use autoclockin;

create table if not exists clock_in_info
(
    id             bigint auto_increment comment 'id'
        primary key,
    clockInAccount varchar(256)                       not null comment '打卡账号',
    clockPassword  varchar(256)                       not null comment '打卡密码',
    userId         bigint                             not null comment '创建用户',
    address        varchar(256)                       not null comment '打卡地址',
    deviceType     varchar(256)                       not null comment '设备型号',
    deviceId       varchar(256)                       not null comment '设备ID',
    longitude      varchar(256)                       not null comment '经度',
    latitude       varchar(256)                       not null comment '纬度',
    clockInTime    varchar(256)                       null comment ' 打卡时间',
    status         tinyint  default 0                 not null comment '打卡状态( 0-未开始 1-已打卡)',
    isEnable       tinyint  default 0                 not null comment '是否启用IP池( 0-关闭 1-开启)',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除'
)
    comment '打卡信息表';

create table if not exists daily_check_in
(
    id          bigint auto_increment comment 'id'
        primary key,
    userId      bigint                             not null comment '签到人',
    description varchar(256)                       null comment '描述',
    status      tinyint  default 0                 not null comment '打卡状态( 0-打卡失败 1-已打卡)',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '每日签到表';

create table if not exists user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userName     varchar(256)                           null comment '用户昵称',
    userAccount  varchar(256)                           not null comment '账号',
    userAvatar   varchar(1024)                          null comment '用户头像',
    gender       tinyint                                null comment '性别',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    userPassword varchar(512)                           not null comment '密码',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    email        varchar(256)                           null comment '邮箱',
    constraint uni_userAccount
        unique (userAccount)
)
    comment '用户';



