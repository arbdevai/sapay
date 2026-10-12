package com.arbdev.sapay.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BannerItem(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("image_desktop") val imageDesktop: String? = null,
    @SerialName("image_mobile") val imageMobile: String? = null,
    @SerialName("mobile_apps_image") val mobileAppsImage: String? = null,
    @SerialName("status") val status: String? = null
) {
    val bestImageUrl: String?
        get() = mobileAppsImage?.takeIf { it.isNotBlank() }
            ?: imageMobile?.takeIf { it.isNotBlank() }
            ?: imageDesktop?.takeIf { it.isNotBlank() }
}

@Serializable
data class BannerResponse(
    @SerialName("message") val message: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("data") val data: List<BannerItem> = emptyList(),
    @SerialName("total") val total: Int? = 0
)

val defaultPromoBanners = listOf(
    BannerItem(
        id = "default-1",
        name = "STARLITE SUPER",
        description = "Internet Super Cepat & Stabil Tanpa Kuota",
        url = "https://starliteindonesia.com/",
        imageMobile = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1784033049744-210101-1000473974.webp",
        imageDesktop = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1784018117041-1322-SUPER_WEB.png",
        status = "publish"
    ),
    BannerItem(
        id = "default-2",
        name = "STARLITE PRIME",
        description = "Koneksi Premium untuk Rumah & Kantor",
        url = "https://starliteindonesia.com/",
        imageMobile = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1784033040996-10233-1000473973.webp",
        imageDesktop = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1784018148228-3101-PRIME_WEB.png",
        status = "publish"
    ),
    BannerItem(
        id = "default-3",
        name = "Hiburan Seru CUBMU",
        description = "Streaming Film & Saluran TV Lengkap",
        url = "https://starliteindonesia.com/",
        imageMobile = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1788166330678-1121-ChatGPT_Image_Aug_31,_2026,_03_51_24_PM.png",
        status = "publish"
    ),
    BannerItem(
        id = "default-4",
        name = "SaPay - Bayar Sat-Set",
        description = "Cek Tagihan & Cetak Struk Kasir Bluetooth",
        url = "https://starliteindonesia.com/how-to-pay",
        imageMobile = "https://codify.obs.ap-southeast-4.myhuaweicloud.com/IJE-FTTH_09122024/devel/assets/banner-1747363423443-0112-Mobile_hero_banner_(1).webp",
        status = "publish"
    )
)
