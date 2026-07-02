-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userName     varchar(256)                           null comment '用户昵称',
    userAccount  varchar(256)                           not null comment '账号',
    userAvatar   varchar(1024)                          null comment '用户头像',
    gender       tinyint                                null comment '性别',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    userPassword varchar(512)                           not null comment '密码',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uni_userAccount
        unique (userAccount)
) comment '用户';

-- 帖子表
create table if not exists post
(
    id            bigint auto_increment comment 'id' primary key,
    age           int comment '年龄',
    gender        tinyint  default 0                 not null comment '性别（0-男, 1-女）',
    education     varchar(512)                       null comment '学历',
    place         varchar(512)                       null comment '地点',
    job           varchar(512)                       null comment '职业',
    contact       varchar(512)                       null comment '联系方式',
    loveExp       varchar(512)                       null comment '感情经历',
    content       text                               null comment '内容（个人介绍）',
    photo         varchar(1024)                      null comment '照片地址',
    reviewStatus  int      default 0                 not null comment '状态（0-待审核, 1-通过, 2-拒绝）',
    reviewMessage varchar(512)                       null comment '审核信息',
    viewNum       int                                not null default 0 comment '浏览数',
    thumbNum      int                                not null default 0 comment '点赞数',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
) comment '帖子';

-- 系统全局配置表（固定单条，id=1）
create table if not exists sys_config
(
    id                     tinyint                               not null comment '固定1，仅一条系统配置' primary key,
    plant_name             varchar(64)                           not null comment '植物名称',
    location_code          varchar(32)                           not null comment '和风城市ID（存数据库用）',
    location_name          varchar(64)                           null comment '城市名称（前端展示用）',
    special_note           varchar(200)                          null comment '生长阶段备注：种子/幼苗期等',
    plant_type             tinyint                               null comment '种植方式：1花盆盆栽(20-25cm) 2大盆(30cm以上) 3地栽单株',
    mail_addr              varchar(128)                          null comment '预警接收邮箱',
    scene_type             tinyint                               not null comment '1室外 2室内',
    enable_warn            tinyint     default 0                 not null comment '0关闭预警 1开启预警邮件',
    enable_auto_intervene  tinyint     default 0                 not null comment '0关闭极端自动方案 1开启自动介入',
    current_plan_type      tinyint                               not null comment '1人工方案 2AI常态方案 3极端临时方案',
    update_time            datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '系统全局配置';

-- 初始化系统配置（必须有一条默认记录）
INSERT INTO sys_config (id, plant_name, location_code, location_name, scene_type, current_plan_type)
VALUES (1, '绿萝', '101010100', '北京', 2, 1);

-- 人工基准方案主表（固定单条，id=1）
create table if not exists irr_manual_plan
(
    id          tinyint                               not null comment '固定1，唯一人工方案' primary key,
    remark      varchar(200)                          null comment '方案文字备注',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '人工基准方案主表';

-- 初始化人工方案
INSERT INTO irr_manual_plan (id, remark) VALUES (1, '人工基准浇水方案');

-- AI常态基准方案主表（固定单条，id=1）
create table if not exists irr_ai_plan
(
    id          tinyint                               not null comment '固定1，唯一AI常态方案' primary key,
    prompt      text                                  null comment '生成方案使用的提示词',
    ai_result   text                                  null comment 'AI原始返回JSON',
    remark      varchar(200)                          null comment '方案备注',
    update_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment 'AI常态基准方案主表';

-- 极端天气临时方案主表
create table if not exists irr_temp_plan
(
    id          bigint auto_increment comment '临时方案主键' primary key,
    source_type tinyint                               not null comment '原基准类型：1人工 2AI，过期切回',
    warn_type   varchar(32)                           not null comment '预警类型：暴雨/高温/台风/干旱等',
    warn_level  varchar(16)                           not null comment '预警等级 blue/yellow/orange/red',
    alert_start datetime                               not null comment '极端天气开始时间',
    alert_end   datetime                               not null comment '极端天气失效时间',
    status      tinyint                               not null comment '1生效中 2已过期作废',
    prompt      text                                  null comment '植物+预警整合提示词',
    ai_result   text                                  null comment 'AI临时方案原始JSON',
    create_time datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    index idx_status (status),
    index idx_expire (alert_end)
) comment '极端天气临时方案主表';

-- 浇水时段明细表（所有方案的多组浇水时间、水量）
create table if not exists irr_plan_item
(
    id             bigint auto_increment comment '浇水时段自增ID' primary key,
    parent_id      bigint                                not null comment '关联对应方案的主键ID',
    parent_type    tinyint                               not null comment '方案类型 1人工/2AI常态/3极端临时',
    water_time     time                                  not null comment '每日浇水时刻 HH:mm:00',
    water_duration int                                   not null comment '单次浇水毫升数',
    sort           int         default 0                 null comment '前端展示排序',
    enable         tinyint     default 1                 null comment '该时段是否启用',
    index idx_parent_rel (parent_id, parent_type)
) comment '存储所有方案的多组浇水时间、水量';

-- 初始化人工方案默认时段（早上8点和下午6点）
INSERT INTO irr_plan_item (parent_id, parent_type, water_time, water_duration, sort, enable)
VALUES (1, 1, '08:00:00', 100, 1, 1), (1, 1, '18:00:00', 150, 2, 1);

-- 历史预警记录表
create table if not exists warn_history
(
    id           bigint auto_increment comment '预警记录主键' primary key,
    warn_id      varchar(64)                           not null comment '和风预警唯一标识',
    warn_type    varchar(32)                           not null comment '预警类型',
    warn_level   varchar(16)                           not null comment '预警等级',
    alert_start  datetime                              not null comment '灾害开始时间',
    alert_end    datetime                              not null comment '灾害结束时间',
    desc_text    text                                  null comment '预警详情描述',
    msg_type     varchar(16)                           not null comment 'alert新增/update更新/cancel解除',
    is_valid     tinyint     default 1                 not null comment '是否有效：1有效 0已作废',
    record_time  datetime    default CURRENT_TIMESTAMP null comment '记录时间',
    index idx_warnid (warn_id),
    index idx_time (record_time),
    index idx_valid (is_valid)
) comment '历史预警记录表';

-- 操作日志表
create table if not exists sys_operation_log
(
    id              bigint auto_increment comment '主键' primary key,
    operation_type  varchar(32)                           not null comment '操作类型：simulate_warn/simulate_clear/publish/activate_temp',
    content         varchar(500)                          not null comment '操作详情',
    create_time     datetime    default CURRENT_TIMESTAMP not null comment '操作时间'
) comment '操作日志表';