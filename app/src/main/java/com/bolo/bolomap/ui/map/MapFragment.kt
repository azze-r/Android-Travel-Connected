package com.bolo.bolomap.ui.map

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.bolo.bolomap.R
import com.bolo.bolomap.db.entities.Photo
import com.bolo.bolomap.db.viewmodel.MainActivity
import com.bolo.bolomap.utils.BaseActivity.Companion.PERMISSIONS_READ_LOCATION
import com.bolo.bolomap.utils.BaseFragment
import com.bolo.bolomap.utils.DateUtils
import com.bolo.bolomap.utils.ImageUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.fragment_home.*
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.model.*


class MapFragment : BaseFragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {


    private lateinit var homeViewModel: MapViewModel
    var textInputEditText: TextInputEditText? = null
    var myMarker: Marker? = null
    var imgList: ImageView? = null
    var imageSave: ImageView? = null
    var imageAdd: ImageView? = null
    var cardAlbum: CardView? = null
    var uri: Uri? = null

    var mMapView: MapView? = null
    var myconstraint: CardView? = null
    var currentAlbum:Photo? = null
    lateinit var imgLocation: ImageView
    lateinit var imgDelete: ImageView
    var icon: BitmapDescriptor? = null

    lateinit var photos :ArrayList<Photo>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {


        homeViewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        imageSave = root.findViewById(R.id.imageSave)
        imageAdd = root.findViewById(R.id.imageAdd)
        cardAlbum = root.findViewById(R.id.cardAlbum)
        imgList = root.findViewById(R.id.imgList)
        imgLocation = root.findViewById(R.id.imgLocation)
        imgDelete = root.findViewById(R.id.imgDelete)
        myconstraint = root.findViewById<View>(R.id.constraint) as CardView
        mMapView = root.findViewById(R.id.mapView)
        textInputEditText = root.findViewById(R.id.textInputEditText)

        mMapView!!.onCreate(savedInstanceState)
        mMapView!!.onResume()

        try {
            MapsInitializer.initialize(activity!!.applicationContext)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        mMapView!!.getMapAsync(this)

        imgList!!.setOnClickListener {
            it.findNavController().navigate(R.id.action_navigation_home_to_navigation_dashboard)
        }

        return root
    }

    override fun onMapReady(p0: GoogleMap?) {

        val act = activity as MainActivity

        if (p0 != null) {

            val photoDao = (activity as MainActivity).getDao()

            photoDao!!.getAllPhotos().observe(this,
                Observer {
                    photos = it as ArrayList<Photo>
                    act.mGoogleMap.clear()
                    for (p in photos) {
                        icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_travel_rounded)
                        myMarker = act.mGoogleMap.addMarker(
                            MarkerOptions()
                                .icon(icon)
                                .position(LatLng(p.lat!!, p.long!!)))

                        myMarker!!.tag = p.id
                    }
                })

            act.mGoogleMap = p0
            act.mGoogleMap.setOnMarkerClickListener(this)

            act.mGoogleMap.setOnMapClickListener {
                myconstraint?.visibility = View.GONE
                cardAlbum?.visibility = View.GONE
            }

            act.mGoogleMap.setOnMapLongClickListener {
                DateUtils.long = it.longitude
                DateUtils.lat = it.latitude
                cardAlbum?.visibility = View.VISIBLE
            }

            imgLocation.setOnClickListener {
                act.moove = true
                act.getPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    PERMISSIONS_READ_LOCATION
                )
            }



            imageSave!!.setOnClickListener {
                val photo = Photo(
                    long = DateUtils.long,
                    lat = DateUtils.lat,
                    photo = uri.toString(),
                    date = null,
                    description = null,
                    label = textInputEditText?.text.toString(),
                    id = 0,photos= null
                )
                (activity as MainActivity).insertPhoto(photo)
                cardAlbum!!.visibility = View.GONE
                imageAdd?.setImageResource(R.drawable.baseline_add_photo_alternate_black_48)
                textInputEditText?.text = null

                //updateMarkersMap()

            }

            imageAdd!!.setOnClickListener {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_OPEN_DOCUMENT
                startActivityForResult(Intent.createChooser(intent, "Select picture"), 1)
            }


        }


    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        myconstraint?.visibility = View.VISIBLE
        val photoDao = (activity as MainActivity).getDao()

        photoDao!!.findById(p0?.tag as Int).observe(this,
            Observer {
                if (it != null) {
                    currentAlbum = it
                    DateUtils.lat = it.lat!!
                    DateUtils.long = it.long!!
                    context?.let { it1 ->
                        ImageUtils.loadImageUriResize(
                            it.photo,
                            R.drawable.baseline_add_photo_alternate_black_48,
                            imgAvatar,
                            it1
                        )
                    }

                    if (it.label.isNullOrEmpty())
                        textTitle.text = "No Title"
                    else
                        textTitle.text = it.label

                    imgAvatar?.setOnClickListener {
                        val bundle = bundleOf("albumId" to p0.tag)
                        view?.findNavController()
                            ?.navigate(com.bolo.bolomap.R.id.action_navigation_home_to_navigation_diapo, bundle)
                    }
                }

            })

        imgDelete.setOnClickListener {
            currentAlbum?.let {
                // setup the alert builder
                val builder = AlertDialog.Builder(context!!)
                builder.setMessage("Do you really want to delete this album?")
                    .setPositiveButton("yes"
                    )
                    { _, _ ->
                        (activity as MainActivity).deletePhoto(it)
                        myconstraint?.visibility = View.GONE
                    }
                    .setNegativeButton("no"
                    ) { dialog, id ->
                        dialog.dismiss()
                    }

                // Create the AlertDialog object and return it
                builder.create()
                builder.show()
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                uri = data?.data!!
                context?.let {
                    imageAdd?.let { img ->
                        ImageUtils.loadImageUriResize(uri.toString(),R.drawable.baseline_add_photo_alternate_black_48,img,context!!)
                    }
                }
            }
        }
    }

}