package voidshield.other.extends.logicExtend.voidShield

enum class SensorMode(vararg val params: String) {
    Contains("x","y","sensors","out"),
    Effect("x","y","sensors","out");

    companion object {
        val all = entries.toTypedArray()
    }
}
