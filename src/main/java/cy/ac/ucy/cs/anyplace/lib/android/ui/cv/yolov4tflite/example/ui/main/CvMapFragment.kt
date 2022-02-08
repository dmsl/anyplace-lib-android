// package cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.example.ui.main
//
// import androidx.lifecycle.ViewModelProvider
// import android.os.Bundle
// import androidx.fragment.app.Fragment
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import cy.ac.ucy.cs.anyplace.lib.R
//
// class CvMapFragment : Fragment() {
//
//   companion object {
//     fun newInstance() = CvMapFragment()
//   }
//
//   private lateinit var viewModel: CvMapViewModel
//
//   override fun onCreateView(
//     inflater: LayoutInflater, container: ViewGroup?,
//     savedInstanceState: Bundle?
//   ): View {
//     return inflater.inflate(R.layout.main_fragment, container, false)
//   }
//
//   override fun onActivityCreated(savedInstanceState: Bundle?) {
//     super.onActivityCreated(savedInstanceState)
//     viewModel = ViewModelProvider(this).get(CvMapViewModel::class.java)
//     // TODO: Use the ViewModel
//   }
//
// }