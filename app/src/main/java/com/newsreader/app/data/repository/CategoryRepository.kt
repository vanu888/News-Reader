package com.newsreader.app.data.repository

import com.newsreader.app.data.db.dao.CategoryDao
import com.newsreader.app.data.db.entity.CategoryEntity
import com.newsreader.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    suspend fun getCategoryByName(name: String): Category? =
        categoryDao.getCategoryByName(name)?.toDomain()

    suspend fun createCategory(name: String, color: Long = 0xFF1A73E8): Long {
        val existing = categoryDao.getCategoryByName(name)
        if (existing != null) return existing.id
        return categoryDao.insertCategory(CategoryEntity(name = name, color = color))
    }

    suspend fun deleteCategory(id: Long) {
        categoryDao.deleteCategoryById(id)
    }

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        color = color
    )
}
