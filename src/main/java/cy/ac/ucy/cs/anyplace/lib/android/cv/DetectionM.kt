package cy.ac.ucy.cs.anyplace.lib.android

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName


// LEFTHERE:
// 1. materialize map and store to file
// 2. inspect file
// 3. read map, create detections again...
// 4. parse it and place it on map
// 5. TODO FUTURE: allow editing the map..

// TODO PARSE CV MAP:
// array of: locations x classes.size

// HASHMAP: OCR: "class_location"

data class CvDetection(
    @SerializedName("class")
    val klass: String,
    @SerializedName("ocr")
    val ocr: String?,

    // dimensions?
)

data class CvLocation(
    @SerializedName("latLng")
    val latLng: LatLng,

    @SerializedName("cvDetections")
    val detections: List<CvDetection>
    // @SerializedName("bottom_left_lng")
)

data class DetectionMap(
    @SerializedName("detectionMap")
    val locations: List<CvLocation>
)

