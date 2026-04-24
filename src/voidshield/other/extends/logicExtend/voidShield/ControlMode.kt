package voidshield.other.extends.logicExtend.voidShield

enum class ControlMode(vararg val params: String) {
    Clear("build"),
    Default("build","build","x","y","range"),
    Bind("build","build");

    companion object {
        val all = entries.toTypedArray()
    }
}
