package voidshield.other.extends.logicExtend.voidShield

enum class ControlMode(vararg val params: String) {
    Clear("build"),
    Default("build","build","x","y","range"),
    Bind("build","build");
    //AddZone("type");

    companion object {
        val all = entries.toTypedArray()
    }
}
