package com.ghlzm.iot.system.mapper;

import com.ghlzm.iot.system.dto.RoleMenuBindingDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleMenuMapper {

    @Select("""
            SELECT menu_id
            FROM sys_role_menu
            WHERE role_id = #{roleId}
            ORDER BY menu_id
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
            <script>
            SELECT DISTINCT menu_id
            FROM sys_role_menu
            WHERE role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            ORDER BY menu_id
            </script>
            """)
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Delete("""
            DELETE FROM sys_role_menu
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
            VALUES
            <foreach collection="bindings" item="item" separator=",">
                (#{item.id}, #{item.roleId}, #{item.menuId}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("bindings") List<RoleMenuBindingDTO> bindings);

    @Select("""
            SELECT COUNT(1)
            FROM sys_role_menu
            WHERE menu_id = #{menuId}
            """)
    int countByMenuId(@Param("menuId") Long menuId);
}
