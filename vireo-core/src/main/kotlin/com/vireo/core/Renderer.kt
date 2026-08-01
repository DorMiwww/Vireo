package com.vireo.core

interface Renderer<T> {
    fun render(file: ResolvedFile): VireoResult<T>
}
