package com.ghlzm.iot.system.mapper;

import com.ghlzm.iot.system.dto.UserRoleBindingDTO;
import com.ghlzm.iot.system.dto.UserRoleViewDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Select("""
            SELECT role_id
            FROM sys_user_role
            WHERE user_id = #{userId}
            ORDER BY role_id
            """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT ur.user_id AS userId,
                   r.id AS roleId,
                   r.role_code AS roleCode,
                   r.role_name AS roleName
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
              AND r.deleted = 0
              AND r.status = 1
            ORDER BY ur.user_id, r.create_time, r.id
            </script>
            """)
    List<UserRoleViewDTO> selectRoleViewsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Delete("""
            DELETE FROM sys_user_role
            WHERE user_id = #{userId}
            """)
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("""
            DELETE FROM sys_user_role
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (id, user_id, role_id, create_time)
            VALUES
            <foreach collection="bindings" item="item" separator=",">
                (#{item.id}, #{item.userId}, #{item.roleId}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("bindings") List<UserRoleBindingDTO> bindings);
}
