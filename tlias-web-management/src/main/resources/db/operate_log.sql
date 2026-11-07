-- 操作日志表：记录 Controller 层关键操作的审计信息
-- 说明：tlias 库原本没有这张表，日志模块上线前需要先执行本脚本
CREATE TABLE IF NOT EXISTS operate_log (
    id              INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operate_emp_id  INT UNSIGNED NULL COMMENT '操作人ID',
    operate_time    DATETIME NOT NULL COMMENT '操作时间',
    class_name      VARCHAR(255) NOT NULL COMMENT '类名',
    method_name     VARCHAR(100) NOT NULL COMMENT '方法名',
    method_params   VARCHAR(2000) NULL COMMENT '方法请求参数',
    return_value    VARCHAR(2000) NULL COMMENT '返回值',
    cost_time       BIGINT NOT NULL COMMENT '执行耗时(ms)',
    KEY idx_operate_time (operate_time),
    KEY idx_operate_emp (operate_emp_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '操作日志';
