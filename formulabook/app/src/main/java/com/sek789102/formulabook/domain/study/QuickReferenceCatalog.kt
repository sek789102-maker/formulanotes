package com.sek789102.formulabook.domain.study

import com.sek789102.formulabook.data.model.FormulaBlock
import com.sek789102.formulabook.data.model.NoteWithTags
import com.sek789102.formulabook.data.model.SubjectCategory
import com.sek789102.formulabook.data.model.SubjectEntity
import java.util.UUID

data class QuickReferenceItem(
    val id: String,
    val subjectId: String,
    val title: String,
    val expressionOrRule: String,
    val explanation: String,
    val tip: String,
    val isFormula: Boolean
)

data class SubjectVaultSummary(
    val subject: SubjectEntity,
    val noteCount: Int,
    val cardCount: Int,
    val masteredCount: Int
) {
    val masteryProgress: Float
        get() = if (cardCount <= 0) 0f else (masteredCount.toFloat() / cardCount.toFloat()).coerceIn(0f, 1f)
}

object QuickReferenceCatalog {

    val items: List<QuickReferenceItem> = listOf(
        // คณิตศาสตร์ (MATH)
        QuickReferenceItem(
            id = "math_quadratic",
            subjectId = SubjectCategory.MATH.name,
            title = "สมการกำลังสอง (Quadratic Formula)",
            expressionOrRule = "x = (-b ± √(b² - 4ac)) / (2a)",
            explanation = "ใช้หารากของสมการ ax² + bx + c = 0 เมื่อ a ≠ 0",
            tip = "ตรวจ Discriminant (b² - 4ac): > 0 มี 2 คำตอบ, = 0 มี 1 คำตอบ, < 0 ไม่มีคำตอบจริง",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "math_pythagoras_trig",
            subjectId = SubjectCategory.MATH.name,
            title = "เอกลักษณ์ตรีโกณมิติหลัก",
            expressionOrRule = "sin²θ + cos²θ = 1  |  tanθ = sinθ / cosθ",
            explanation = "sin 30° = 1/2, sin 45° = √2/2, sin 60° = √3/2",
            tip = "จำหลักมือซ้าย 0°, 30°, 45°, 60°, 90°: sin นับนิ้วล่าง √นิ้ว/2, cos นับนิ้วบน",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "math_arithmetic_seq",
            subjectId = SubjectCategory.MATH.name,
            title = "ลำดับและอนุกรมเลขคณิต",
            expressionOrRule = "an = a1 + (n - 1)d  |  Sn = (n / 2) × (a1 + an)",
            explanation = "a1 = พจน์แรก, d = ผลต่างร่วม, n = จำนวนพจน์",
            tip = "ถ้าไม่รู้พจน์สุดท้าย an ให้ใช้ Sn = (n/2) × [2a1 + (n-1)d]",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "math_derivative",
            subjectId = SubjectCategory.MATH.name,
            title = "อนุพันธ์และอินทิกรัลพหุนาม",
            expressionOrRule = "d/dx (x^n) = n × x^(n-1)  |  ∫ x^n dx = x^(n+1)/(n+1) + C",
            explanation = "ใช้ได้เมื่อ n ≠ -1 สำหรับการหาอินทิกรัล",
            tip = "ดิฟค่าคงที่ได้ 0 เสมอ และอย่าลืมบวก +C ทุกครั้งเมื่อหาอินทิกรัลไม่จำกัดเขต",
            isFormula = true
        ),

        // ฟิสิกส์ (PHYSICS)
        QuickReferenceItem(
            id = "phys_suvat",
            subjectId = SubjectCategory.PHYSICS.name,
            title = "การเคลื่อนที่แนวตรง (ความเร่งคงตัว)",
            expressionOrRule = "v = u + at  |  s = ut + 0.5 × a × t²  |  v² = u² + 2as",
            explanation = "u = ความเร็วต้น, v = ความเร็วปลาย, a = ความเร่ง, s = การกระจัด, t = เวลา",
            tip = "เลือกสูตรจากตัวแปรที่โจทย์ 'ไม่ได้บอกและไม่ถามหา' เป็นหลัก",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "phys_newton_energy",
            subjectId = SubjectCategory.PHYSICS.name,
            title = "แรง งาน และพลังงานจลน์",
            expressionOrRule = "F = m × a  |  Ek = 0.5 × m × v²  |  Ep = m × g × h",
            explanation = "m = มวล (kg), v = อัตราเร็ว (m/s), g ≈ 9.8 m/s², h = ความสูง (m)",
            tip = "ถ้าไม่มีแรงเสียดทาน พลังงานกลรวม (Ek + Ep) จะคงที่เสมอ",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "phys_ohm_power",
            subjectId = SubjectCategory.PHYSICS.name,
            title = "ไฟฟ้ากระแสตรง (กฎของโอห์ม)",
            expressionOrRule = "V = I × R  |  P = I × V = I²R = V² / R",
            explanation = "V = แรงดัน (V), I = กระแส (A), R = ความต้านทาน (Ω), P = กำลังไฟฟ้า (W)",
            tip = "ต่ออนุกรม: I เท่ากัน, R รวม = R1 + R2 | ต่อขนาน: V เท่ากัน",
            isFormula = true
        ),

        // เคมี (CHEMISTRY)
        QuickReferenceItem(
            id = "chem_mole",
            subjectId = SubjectCategory.CHEMISTRY.name,
            title = "ความสัมพันธ์จำนวนโมล (n)",
            expressionOrRule = "n = g / MW = (C × V_cm3) / 1000 = V_STP / 22.4",
            explanation = "g = มวลสาร (กรัม), MW = มวลโมเลกุล, C = ความเข้มข้น (mol/L)",
            tip = "ปริมาตรแก๊ส 1 โมลที่ STP (0°C, 1 atm) เท่ากับ 22.4 ลิตรเสมอ",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "chem_ideal_gas",
            subjectId = SubjectCategory.CHEMISTRY.name,
            title = "กฎแก๊สอุดมคติและค่า pH",
            expressionOrRule = "P × V = n × R × T  |  pH = -log10(H_plus)",
            explanation = "R = 0.0821 L·atm/(mol·K), T = อุณหภูมิเคลวิน (°C + 273)",
            tip = "ระวังหน่วย P ต้องเป็น atm, V เป็นลิตร และ T เป็นเคลวินเสมอ",
            isFormula = true
        ),
        QuickReferenceItem(
            id = "chem_ions",
            subjectId = SubjectCategory.CHEMISTRY.name,
            title = "กลุ่มไอออนหลายอะตอมที่ออกสอบบ่อย",
            expressionOrRule = "NH₄⁺ (แอมโมเนียม) · OH⁻ (ไฮดรอกไซด์) · NO₃⁻ (ไนเตรต) · SO₄²⁻ (ซัลเฟต) · CO₃²⁻ (คาร์บอเนต)",
            explanation = "ประจุ +1, -1, -2 ช่วยดุลสูตรสารประกอบไอออนิกได้ทันที",
            tip = "ไขว้ประจุลงมาเป็นเลขห้อยแล้วทอนเป็นอัตราส่วนอย่างต่ำ",
            isFormula = false
        ),

        // ชีววิทยา (BIOLOGY)
        QuickReferenceItem(
            id = "bio_photosynthesis",
            subjectId = SubjectCategory.BIOLOGY.name,
            title = "สังเคราะห์ด้วยแสง vs หายใจระดับเซลล์",
            expressionOrRule = "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂  (คลอโรพลาสต์)",
            explanation = "หายใจระดับเซลล์แบบใช้ออกซิเจนได้พลังงาน ~36–38 ATP ในไมโทคอนเดรีย",
            tip = "จำง่ายๆ: Light reaction เกิดที่ไทลาคอยด์, Calvin cycle เกิดที่สโตรมา",
            isFormula = false
        ),
        QuickReferenceItem(
            id = "bio_mitosis_meiosis",
            subjectId = SubjectCategory.BIOLOGY.name,
            title = "การแบ่งเซลล์ Mitosis vs Meiosis",
            expressionOrRule = "Mitosis: 1 เซลล์ → 2 เซลล์ (2n เหมือนเดิม) | Meiosis: 1 เซลล์ → 4 เซลล์ (n ลดครึ่ง)",
            explanation = "Mitosis ใช้ซ่อมแซมและเจริญเติบโต ส่วน Meiosis ใช้สร้างเซลล์สืบพันธุ์",
            tip = "Crossing over เกิดเฉพาะระยะ Prophase I ของ Meiosis เพื่อเพิ่มความหลากหลาย",
            isFormula = false
        ),

        // ภาษาอังกฤษ (ENGLISH)
        QuickReferenceItem(
            id = "eng_tenses",
            subjectId = SubjectCategory.ENGLISH.name,
            title = "สูตรโครงสร้าง Tense หลักที่ใช้บ่อย",
            expressionOrRule = "Simple: S+V1/V2 | Cont.: S+is/am/are/was/were+V.ing | Perf.: S+have/has/had+V3",
            explanation = "Present Perfect (have/has + V3) ใช้กับเหตุการณ์เริ่มในอดีตต่อเนื่องถึงปัจจุบัน",
            tip = "เจอ since / for / already / yet / just มักใช้ Present Perfect",
            isFormula = false
        ),
        QuickReferenceItem(
            id = "eng_if_clause",
            subjectId = SubjectCategory.ENGLISH.name,
            title = "Conditional Sentences (If-Clause 4 แบบ)",
            expressionOrRule = "0: V1 → V1 | 1: V1 → will+V.inf | 2: V2/were → would+V.inf | 3: had+V3 → would have+V3",
            explanation = "แบบ 2 สมมติตรงข้ามปัจจุบัน (ใช้ were กับทุกประธาน) · แบบ 3 สมมติตรงข้ามอดีต",
            tip = "จำคู่กริยาให้แม่น: V1 คู่ will, V2 คู่ would, had+V3 คู่ would have+V3",
            isFormula = false
        ),
        QuickReferenceItem(
            id = "eng_passive_voice",
            subjectId = SubjectCategory.ENGLISH.name,
            title = "หัวใจของ Passive Voice (ประธานถูกกระทำ)",
            expressionOrRule = "S + Verb to be (ตาม Tense) + V3 (+ by ...)",
            explanation = "เช่น Present: is/are + V3 · Past: was/were + V3 · Modal: will/can + be + V3",
            tip = "เช็กความหมายประธานก่อนเสมอ ถ้าทำเองไม่ได้ต้องมี be + V3",
            isFormula = false
        ),

        // ภาษาไทย (THAI)
        QuickReferenceItem(
            id = "thai_kamtai",
            subjectId = SubjectCategory.THAI.name,
            title = "หลักจำ คำเป็น – คำตาย & ไตรยางศ์",
            expressionOrRule = "คำเป็น: สระเสียงยาว (แม่ ก กา) / สะกดแม่ นมยวง | คำตาย: สระเสียงสั้น / สะกดแม่ กบด",
            explanation = "อักษรกลาง (ไก่ จิก เด็ก ตาย บน ปาก โอ่ง) ผันได้ครบ 5 เสียงทั้งคำเป็น",
            tip = "พยางค์สะกด ก บ ด = คำตาย (กบฏ) · พยางค์สะกด น ม ย ว ง = คำเป็น (นมยวง)",
            isFormula = false
        ),
        QuickReferenceItem(
            id = "thai_chantalak",
            subjectId = SubjectCategory.THAI.name,
            title = "ฉันทลักษณ์กลอนสุภาพ (กลอนแปด)",
            expressionOrRule = "บทละ 4 วรรค (สดับ รับ รอง ส่ง) · วรรคละ 7–9 คำ · ส่งสัมผัสท้ายวรรค",
            explanation = "เสียงท้ายวรรครับหลีกเลี่ยงเสียงสามัญ นิยมเสียงจัตวา, วรรครอง-ส่งมักลงสามัญ/ตรี",
            tip = "จำลำดับวรรค: สดับ → รับ → รอง → ส่ง และตรวจสัมผัสระหว่างบททุกครั้ง",
            isFormula = false
        ),

        // สังคมศึกษา & ประวัติศาสตร์ (SOCIAL / HISTORY)
        QuickReferenceItem(
            id = "soc_supply_demand",
            subjectId = SubjectCategory.SOCIAL.name,
            title = "กฎอุปสงค์ (Demand) และอุปทาน (Supply)",
            expressionOrRule = "ราคา↑ → อุปสงค์(ซื้อ)↓ แต่อุปทาน(ขาย)↑  |  จุดดุลยภาพ: Qd = Qs",
            explanation = "อุปสงค์แปรผกผันกับราคา ส่วนอุปทานแปรผันตรงกับราคา",
            tip = "จำง่ายๆ: Demand = ผู้บริโภคอยากซื้อ (แพงซื้อน้อย) · Supply = ผู้ผลิตอยากขาย (แพงอยากขายมาก)",
            isFormula = false
        ),
        QuickReferenceItem(
            id = "hist_thai_eras",
            subjectId = SubjectCategory.HISTORY.name,
            title = "ลำดับราชธานีและยุคประวัติศาสตร์ไทย",
            expressionOrRule = "สุโขทัย (พ.ศ.1792–1981) → อยุธยา (พ.ศ.1893–2310) → ธนบุรี (พ.ศ.2310–2325) → รัตนโกสินทร์ (พ.ศ.2325–ปัจจุบัน)",
            explanation = "ช่วยเทียบเหตุการณ์สำคัญ รัชกาล และการปฏิรูปประเทศได้รวดเร็ว",
            tip = "อยุธยา 417 ปี · ธนบุรี 15 ปี · รัตนโกสินทร์เริ่ม พ.ศ. 2325",
            isFormula = false
        ),

        // คอมพิวเตอร์ (COMPUTER)
        QuickReferenceItem(
            id = "cs_binary_logic",
            subjectId = SubjectCategory.COMPUTER.name,
            title = "ระบบเลขฐานและลอจิกเกตพื้นฐาน",
            expressionOrRule = "1 Byte = 8 Bits (0–255)  |  AND: จริงเมื่อจริงคู่  |  OR: เท็จเมื่อเท็จคู่",
            explanation = "แปลงฐาน 2 เป็นฐาน 16 ให้แบ่งทีละ 4 บิต (เช่น 1010₂ = A₁₆ = 10₁₀)",
            tip = "จำค่า 2ⁿ ที่เจอบ่อย: 2⁸ = 256, 2¹⁰ = 1024 (1 KB)",
            isFormula = false
        )
    )

    fun availableSubjectIds(): List<String> =
        items.map { it.subjectId }.distinct()

    fun filterItems(subjectId: String?, query: String): List<QuickReferenceItem> {
        val cleanQuery = query.trim()
        return items.filter { item ->
            val matchesSubject = subjectId == null || item.subjectId == subjectId
            val matchesQuery = cleanQuery.isEmpty() ||
                item.title.contains(cleanQuery, ignoreCase = true) ||
                item.expressionOrRule.contains(cleanQuery, ignoreCase = true) ||
                item.explanation.contains(cleanQuery, ignoreCase = true) ||
                item.tip.contains(cleanQuery, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }

    fun summarizeSubjects(
        subjects: List<SubjectEntity>,
        notes: List<NoteWithTags>
    ): List<SubjectVaultSummary> {
        val grouped = notes.groupBy { it.note.subject }
        return subjects.map { subject ->
            val subjectNotes = grouped[subject.id].orEmpty()
            val allBlocks = subjectNotes.flatMap { it.note.formulas }.filter { it.expression.isNotBlank() }
            val mastered = allBlocks.count { it.mastered }
            SubjectVaultSummary(
                subject = subject,
                noteCount = subjectNotes.size,
                cardCount = allBlocks.size,
                masteredCount = mastered
            )
        }.sortedWith(
            compareByDescending<SubjectVaultSummary> { it.noteCount > 0 }
                .thenByDescending { it.noteCount }
                .thenBy { it.subject.sortOrder }
        )
    }

    fun toNoteDraftContent(item: QuickReferenceItem): Pair<String, FormulaBlock> {
        val content = buildString {
            appendLine("# ${item.title}")
            appendLine("- ${item.explanation}")
            if (item.tip.isNotBlank()) {
                appendLine()
                appendLine("# ทริคจำไว")
                appendLine("- ==${item.tip}==")
            }
        }.trim()
        val block = FormulaBlock(
            id = UUID.randomUUID().toString(),
            title = item.title,
            expression = item.expressionOrRule,
            variablesNote = item.explanation,
            mastered = false,
            blockType = if (item.isFormula) FormulaBlock.BLOCK_TYPE_FORMULA else FormulaBlock.BLOCK_TYPE_FLASHCARD
        )
        return content to block
    }
}
