package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dept {
    private Integer id; // ID
    private String name; // Name
    private LocalDateTime createTime; // Create Time
    private LocalDateTime updateTime; // Update Time
}
