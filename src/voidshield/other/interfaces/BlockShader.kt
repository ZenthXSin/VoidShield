package voidshield.other.interfaces

import arc.util.Disposable


interface BlockShader: Disposable {
    var hasShader: Boolean
    fun setGradient()
    fun getMeshId(): String
    fun setShader()
}