package live.hms.roomkit.ui.theme

import com.google.gson.annotations.SerializedName

//todo move to colors.xml
data class DefaultDarkThemeColours(
    @SerializedName("primary_default") val primary_default: String = "#2572ED",
    @SerializedName("primary_bright") val primary_bright: String = "#538DFF",
    @SerializedName("primary_dim") val primary_dim: String = "#002D6D",
    @SerializedName("background_dim") val background_dim : String = "#000000",
    @SerializedName("primary_disabled") val primary_disabled: String = "#004299",
    @SerializedName("secondary_default") val secondary_default: String = "#444954",
    @SerializedName("secondary_bright") val secondary_bright: String = "#70778B",
    @SerializedName("secondary_dim") val secondary_dim: String = "#293042",
    @SerializedName("secondary_disabled") val secondary_disabled: String = "#404759",
    @SerializedName("surface_default") val surface_default: String = "#191B23",
    @SerializedName("surface_bright") val surface_bright: String = "#272A31",
    @SerializedName("surface_dim") val surface_dim: String = "#2E3038",
    @SerializedName("surface_disabled") val surface_disabled: String = "#11131A",
    @SerializedName("onprimary_high_emp") val onprimary_high_emp: String = "#FFFFFF",
    @SerializedName("onprimary_med_emp") val onprimary_med_emp: String = "#CCDAFF",
    @SerializedName("onprimary_low_emp") val onprimary_low_emp: String = "#84AAFF",
    @SerializedName("onsecondary_high_emp") val onsecondary_high_emp: String = "#FFFFFF",
    @SerializedName("onsecondary_med_emp") val onsecondary_med_emp: String = "#D3D9F0",
    @SerializedName("onsecondary_low_emp") val onsecondary_low_emp: String = "#A4ABC0",
    @SerializedName("onsurface_high_emp") val onsurface_high_emp: String = "#EFF0FA",
    @SerializedName("onsurface_med_emp") val onsurface_med_emp: String = "#C5C6D0",
    @SerializedName("onsurface_low_emp") val onsurface_low_emp: String = "#8F9099",
    @SerializedName("error_default") val error_default: String = "#C74E5B",
    @SerializedName("error_container") val error_container: String = "#FFB2B6",
    @SerializedName("background_default") val background_default: String = "#0B0E15",
    @SerializedName("border_bright") val border_bright: String = "#272A31"
)

