package com.suseoaa.projectoaa.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    @SerialName("jfckbkg")
    val jfckbkg: Boolean,
    @SerialName("kbList")
    val kbList: List<Kb>,
    @SerialName("kblx")
    val kblx: Int,
    @SerialName("qsxqj")
    val qsxqj: String,
    @SerialName("sfxsd")
    val sfxsd: String,
    @SerialName("sjfwkg")
    val sxgykbbz: String,
    @SerialName("xkkg")
    val xkkg: Boolean,
    @SerialName("xnxqsfkz")
    val xnxqsfkz: String,
    @SerialName("xsbjList")
    val xsbjList: List<Xsbj>,
    @SerialName("xsxx")
    val xsxx: Xsxx,
    @SerialName("zckbsfxssj")
    val zckbsfxssj: String
) {
    @Serializable
    data class Kb(
        @SerialName("bklxdjmc")
        val bklxdjmc: String,
        @SerialName("cd_id")
        val cdId: String,
        @SerialName("cdbh")
        val cdbh: String,
        @SerialName("cdlbmc")
        val cdlbmc: String,
        @SerialName("cdmc")
        val cdmc: String,
        @SerialName("cxbj")
        val cxbj: String,
        @SerialName("cxbjmc")
        val cxbjmc: String,
        @SerialName("date")
        val date: String,
        @SerialName("dateDigit")
        val dateDigit: String,
        @SerialName("dateDigitSeparator")
        val dateDigitSeparator: String,
        @SerialName("day")
        val day: String,
        @SerialName("jc")
        val jc: String,
        @SerialName("jcor")
        val jcor: String,
        @SerialName("jcs")
        val jcs: String,
        @SerialName("jgh_id")
        val jghId: String,
        @SerialName("jgpxzd")
        val jgpxzd: String,
        @SerialName("jxb_id")
        val jxbId: String,
        @SerialName("jxbmc")
        val jxbmc: String,
        @SerialName("jxbzc")
        val jxbzc: String,
        @SerialName("kcbj")
        val kcbj: String,
        @SerialName("kch")
        val kch: String,
        @SerialName("kch_id")
        val kchId: String,
        @SerialName("kclb")
        val kclb: String,
        @SerialName("kcmc")
        val kcmc: String,
        @SerialName("kcxszc")
        val kcxszc: String,
        @SerialName("kcxz")
        val kcxz: String,
        @SerialName("kczxs")
        val kczxs: String,
        @SerialName("khfsmc")
        val khfsmc: String,
        @SerialName("kklxdm")
        val kklxdm: String,
        @SerialName("kkzt")
        val kkzt: String,
        @SerialName("ksfsmc")
        val ksfsmc: String,
        @SerialName("lh")
        val lh: String,
        @SerialName("listnav")
        val listnav: String,
        @SerialName("localeKey")
        val localeKey: String,
        @SerialName("month")
        val month: String,
        @SerialName("njxh")
        val njxh: Int,
        @SerialName("oldjc")
        val oldjc: String,
        @SerialName("oldzc")
        val oldzc: String,
        @SerialName("pageTotal")
        val pageTotal: Int,
        @SerialName("pageable")
        val pageable: Boolean,
        @SerialName("pkbj")
        val pkbj: String,
        @SerialName("px")
        val px: String,
        @SerialName("qqqh")
        val qqqh: String,
        @SerialName("rangeable")
        val rangeable: Boolean,
        @SerialName("rk")
        val rk: String,
        @SerialName("rsdzjs")
        val rsdzjs: Int,
        @SerialName("sfjf")
        val sfjf: String,
        @SerialName("sfkckkb")
        val sfkckkb: Boolean,
        @SerialName("skfsmc")
        val skfsmc: String,
        @SerialName("sxbj")
        val sxbj: String,
        @SerialName("totalResult")
        val totalResult: String,
        @SerialName("xf")
        val xf: String,
        @SerialName("xkbz")
        val xkbz: String,
        @SerialName("xkrs")
        val xkrs: String,
        @SerialName("xm")
        val xm: String,
        @SerialName("xnm")
        val xnm: String,
        @SerialName("xqdm")
        val xqdm: String,
        @SerialName("xqh1")
        val xqh1: String,
        @SerialName("xqh_id")
        val xqhId: String,
        @SerialName("xqj")
        val xqj: String,
        @SerialName("xqjmc")
        val xqjmc: String,
        @SerialName("xqm")
        val xqm: String,
        @SerialName("xqmc")
        val xqmc: String,
        @SerialName("xsdm")
        val xsdm: String,
        @SerialName("xslxbj")
        val xslxbj: String,
        @SerialName("year")
        val year: String,
        @SerialName("zcd")
        val zcd: String,
        @SerialName("zcmc")
        val zcmc: String,
        @SerialName("zfjmc")
        val zfjmc: String,
        @SerialName("zhxs")
        val zhxs: String,
        @SerialName("zxs")
        val zxs: String,
        @SerialName("zxxx")
        val zxxx: String,
        @SerialName("zyfxmc")
        val zyfxmc: String,
        @SerialName("zyhxkcbj")
        val zyhxkcbj: String,
        @SerialName("zzmm")
        val zzmm: String,
        @SerialName("zzrl")
        val zzrl: String
    )

    @Serializable
    data class Xsbj(
        @SerialName("xsdm")
        val xsdm: String,
        @SerialName("xslxbj")
        val xslxbj: String,
        @SerialName("xsmc")
        val xsmc: String
    )

    @Serializable
    data class Xsxx(
        @SerialName("BJMC")
        val bJMC: String,
        @SerialName("JFZT")
        val jFZT: Int,
        @SerialName("KCMS")
        val kCMS: Int,
        @SerialName("KXKXXQ")
        val kXKXXQ: String,
        @SerialName("NJDM_ID")
        val nJDMID: String,
        @SerialName("XH")
        val xH: String,
        @SerialName("XH_ID")
        val xHID: String,
        @SerialName("XKKG")
        val xKKG: String,
        @SerialName("XKKGXQ")
        val xKKGXQ: String,
        @SerialName("XM")
        val xM: String,
        @SerialName("XNM")
        val xNM: String,
        @SerialName("XNMC")
        val xNMC: String,
        @SerialName("XQM")
        val xQM: String,
        @SerialName("XQMMC")
        val xQMMC: String,
        @SerialName("ZYH_ID")
        val zYHID: String,
        @SerialName("ZYMC")
        val zYMC: String
    )
}
