package com.sek789102.changkhamnuan.domain

/** Unit conversion, including Thai land and length units. Factors are to the base unit of each group. */
object Units {
    data class Unit(val id: String, val label: String, val toBase: Double)
    data class Group(val id: String, val label: String, val units: List<Unit>)

    val groups = listOf(
        Group(
            "length", "ความยาว",
            listOf(
                Unit("m", "เมตร", 1.0), Unit("cm", "เซนติเมตร", 0.01), Unit("mm", "มิลลิเมตร", 0.001),
                Unit("wa", "วา", 2.0), Unit("ft", "ฟุต", 0.3048), Unit("in", "นิ้ว", 0.0254), Unit("km", "กิโลเมตร", 1000.0)
            )
        ),
        Group(
            "area", "พื้นที่",
            listOf(
                Unit("m2", "ตารางเมตร", 1.0), Unit("wa2", "ตารางวา", 4.0), Unit("ngan", "งาน", 400.0),
                Unit("rai", "ไร่", 1600.0), Unit("ft2", "ตารางฟุต", 0.09290304), Unit("cm2", "ตารางเซนติเมตร", 0.0001)
            )
        ),
        Group(
            "volume", "ปริมาตร",
            listOf(
                Unit("m3", "ลูกบาศก์เมตร (คิว)", 1.0), Unit("l", "ลิตร", 0.001), Unit("gal", "แกลลอน (US)", 0.003785411784),
                Unit("ft3", "ลูกบาศก์ฟุต", 0.028316846592)
            )
        ),
        Group(
            "weight", "น้ำหนัก",
            listOf(Unit("kg", "กิโลกรัม", 1.0), Unit("t", "ตัน", 1000.0), Unit("g", "กรัม", 0.001), Unit("lb", "ปอนด์", 0.45359237))
        )
    )

    fun convert(value: Double, from: Unit, to: Unit): Double = value * from.toBase / to.toBase

    /** 1 ไร่ 2 งาน 50 ตร.วา style text for a size in square metres. */
    fun thaiLand(m2: Double): String {
        val wa2Total = m2 / 4.0
        val rai = (wa2Total / 400).toLong()
        val ngan = ((wa2Total - rai * 400) / 100).toLong()
        val wa2 = wa2Total - rai * 400 - ngan * 100
        return "$rai ไร่ $ngan งาน ${Fmt.n(wa2)} ตร.วา"
    }
}
