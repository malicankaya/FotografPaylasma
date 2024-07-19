package com.malicankaya.fotografpaylasma.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.malicankaya.fotografpaylasma.databinding.FragmentYuklemeBinding
import java.util.UUID

class YuklemeFragment : Fragment() {

    private var _binding: FragmentYuklemeBinding? = null
    private val binding get() = _binding!!

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var secilenGorsel: Uri? = null
    private var secilenBitmap: Bitmap? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        storage = Firebase.storage
        db = Firebase.firestore

        registerLauncher()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentYuklemeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { postYukle(it) }
        binding.imageView.setOnClickListener { gorselSec(it) }
    }

    private fun postYukle(view: View) {
        val uuid = UUID.randomUUID().toString()
        val gorselAdi = uuid + ".jpg"


        if (secilenGorsel != null) {
            val reference = storage.reference
            val gorselReference = reference.child("images").child(gorselAdi)
            val uploadTask = gorselReference.putFile(secilenGorsel!!)
            uploadTask.addOnSuccessListener {
                //url alınacak
                gorselReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()
                    //println(downloadUrl)

                    //post yüklenecek, veri tabanın kayıt yapılacak
                    if (auth.currentUser != null) {
                        val post = hashMapOf<String, Any>()

                        post.put("email", auth.currentUser!!.email.toString())
                        post.put("comment", binding.commentEditText.text.toString())
                        post.put("imageUrl", downloadUrl)
                        post.put("date", Timestamp.now())

                        db.collection("Posts")
                            .add(post)
                            .addOnSuccessListener {

                                val action =
                                    YuklemeFragmentDirections.actionYuklemeFragmentToFeedFragment()
                                Navigation.findNavController(view).navigate(action)

                            }.addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    it.localizedMessage.toString(),
                                    Toast.LENGTH_LONG
                                ).show()

                            }
                    }
                }

            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Bir hata oluştu: " + it.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    private fun gorselSec(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestGalleryAccess(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestGalleryAccess(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requestGalleryAccess(permission: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //izin verilmemiş, izin istenecek
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permission
                )
            ) {
                //snackbar ile sorup izin isteyeceğiz
                Snackbar.make(
                    requireView(),
                    "Görsel izni gereklidir, lütfen izin veriniz.",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(
                        "İzin ver",
                        View.OnClickListener {
                            //izin isteyeceğiz
                            permissionLauncher.launch(permission)
                        }
                    ).show()
            } else {
                //izin isteyeceğiz
                permissionLauncher.launch(permission)
            }
        } else {
            //izin verilmiş, galeriye gidilecek
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    fun registerLauncher() {

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //galeriye gideceğiz
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                } else {
                    //kullanici izni reddetti
                    Toast.makeText(requireContext(), "İzin reddedildi", Toast.LENGTH_LONG).show()
                }
            }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        secilenGorsel = intentFromResult.data
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(
                                requireActivity().contentResolver,
                                secilenGorsel!!
                            )
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                        } else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().contentResolver,
                                secilenGorsel
                            )
                        }
                        binding.imageView.setImageBitmap(secilenBitmap)
                    }
                }
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}