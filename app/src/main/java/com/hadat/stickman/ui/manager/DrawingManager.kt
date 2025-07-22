package com.hadat.stickman.ui.manager

import com.hadat.stickman.ui.home.DrawingView

class DrawingManager {

    private val drawingMap = mutableMapOf<Int, DrawingView>()

    // Thêm một DrawingView với ID
    fun addDrawingView(id: Int, view: DrawingView) {
        drawingMap[id] = view
    }

    // Lấy DrawingView theo ID
    fun getDrawingView(id: Int): DrawingView? {
        return drawingMap[id]
    }

    // Xoá một DrawingView theo ID
    fun removeDrawingView(id: Int) {
        drawingMap.remove(id)
    }

    // Kiểm tra xem đã có DrawingView với ID đó chưa
    fun contains(id: Int): Boolean {
        return drawingMap.containsKey(id)
    }

    // Lấy danh sách tất cả ID
    fun getAllIds(): Set<Int> {
        return drawingMap.keys
    }

    // Xoá tất cả
    fun clear() {
        drawingMap.clear()
    }
}
