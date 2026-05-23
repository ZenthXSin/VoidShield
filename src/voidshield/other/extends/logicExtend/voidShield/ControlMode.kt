package voidshield.other.extends.logicExtend.voidShield

enum class ControlMode(vararg val params: String) {
    Default("build","build","x","y","range"),
    Bind("build","build"),
    UpdateZone("x","y","tx","ty","build"),
    CircleZone("x","y","range"),
    PolygonZone("x","y"),
    ClearZone("build"),
    AutoBlock("build","build"),
    UseZone("build");

    companion object {
        val all = entries.toTypedArray()
    }
}
