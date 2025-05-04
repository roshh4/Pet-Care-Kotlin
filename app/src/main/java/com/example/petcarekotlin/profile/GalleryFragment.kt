package com.example.petcarekotlin.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petcarekotlin.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GalleryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GalleryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var addPhotoButton: MaterialButton
    private val db = Firebase.firestore
    private var petId: String? = null
    private var galleryImages = mutableListOf<String>()
    private lateinit var adapter: GalleryAdapter

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GalleryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GalleryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.galleryRecyclerView)
        addPhotoButton = view.findViewById(R.id.addPhotoButton)

        // Setup RecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        adapter = GalleryAdapter(galleryImages)
        recyclerView.adapter = adapter

        // Get current pet ID from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE)
        petId = sharedPrefs.getString("CURRENT_PET_ID", null)

        // Load gallery images
        loadGalleryImages()

        // Set click listener for add photo button
        addPhotoButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun loadGalleryImages() {
        if (petId == null) return

        db.collection("pets").document(petId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val images = document.get("galleryImages") as? List<String> ?: listOf()
                    galleryImages.clear()
                    galleryImages.addAll(images)
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                // Convert bitmap to Base64 string
                val byteArrayOutputStream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                val imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                // Save to Firestore
                saveImageToGallery(imageString)
            }
        }
    }

    private fun saveImageToGallery(imageString: String) {
        if (petId == null) return

        db.collection("pets").document(petId!!)
            .update("galleryImages", FieldValue.arrayUnion(imageString))
            .addOnSuccessListener {
                Toast.makeText(context, "Image added to gallery", Toast.LENGTH_SHORT).show()
                galleryImages.add(imageString)
                adapter.notifyItemInserted(galleryImages.size - 1)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class GalleryAdapter(private val images: List<String>) :
        RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.galleryImage)
            val placeholder: ImageView = view.findViewById(R.id.placeholderIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position < images.size) {
                try {
                    val imageBytes = Base64.decode(images[position], Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.imageView.setImageBitmap(bitmap)
                    holder.imageView.visibility = View.VISIBLE
                    holder.placeholder.visibility = View.GONE
                } catch (e: Exception) {
                    holder.imageView.visibility = View.GONE
                    holder.placeholder.visibility = View.VISIBLE
                }
            } else {
                holder.imageView.visibility = View.GONE
                holder.placeholder.visibility = View.VISIBLE
            }
        }

        override fun getItemCount() = images.size
    }
} 