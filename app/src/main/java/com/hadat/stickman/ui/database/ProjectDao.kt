package com.hadat.stickman.ui.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

// Giao diện DAO để thao tác với bảng projects trong cơ sở dữ liệu
@Dao
interface ProjectDao {
    // Chèn một ProjectEntity vào bảng projects
    @Insert
    suspend fun insert(project: ProjectEntity)

    // Cập nhật một ProjectEntity trong bảng projects
    @Update
    suspend fun update(project: ProjectEntity)

    // Xóa một ProjectEntity
    @Delete
    suspend fun delete(project: ProjectEntity)

    // Xóa dự án theo id
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Int)

    // Lấy ID lớn nhất từ bảng projects
    @Query("SELECT MAX(id) FROM projects")
    suspend fun getMaxId(): Int?

    // Lấy một ProjectEntity theo tên dự án
    @Query("SELECT * FROM projects WHERE name = :name")
    suspend fun getProjectByName(name: String): ProjectEntity?

    // Lấy tất cả các ProjectEntity từ bảng projects
    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<ProjectEntity>
}
