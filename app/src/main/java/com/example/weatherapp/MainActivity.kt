package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.GridView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.airbnb.lottie.LottieAnimationView
import com.example.weatherapp.datamodel.RetrofitServices
import com.example.weatherapp.datamodel.WeatherApp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    val weatherLiveData = MutableLiveData<WeatherApp>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val dataListLiveData = MutableLiveData<List<GridItems>>()
    private lateinit var root: ConstraintLayout
    private lateinit var search: SearchView
    private lateinit var city: TextView
    private lateinit var temp: TextView
    private lateinit var tempMax: TextView
    private lateinit var tempMin: TextView
    private lateinit var day: TextView
    private lateinit var date: TextView
    private lateinit var lottie: LottieAnimationView
    private lateinit var weatherType: TextView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)
        root = findViewById(R.id.rootView)
        root.alpha = 0.2f

        progressBar.visibility = View.VISIBLE
        val gridView: GridView = findViewById(R.id.gridView)
        gridView(gridView)
        init()
        search = findViewById(R.id.searchViewID)
        city = findViewById(R.id.cityTextView)
        temp = findViewById(R.id.celciusTextView)
        tempMax = findViewById(R.id.maxtempTextView)
        tempMin = findViewById(R.id.mintempTextView)
        weatherType = findViewById(R.id.weatherTypeTextView)
        day = findViewById(R.id.dayTextView)
        date = findViewById(R.id.dateTextView)
        lottie = findViewById(R.id.lottie)

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Handle search query submission
                retrofit(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Handle query text change (as the user types)
                // You can implement live search functionality here
                return true
            }
        })

        weatherLiveData.observe(this) {
            if (it != null) {
                city.text = it.name
                temp.text = it.main.temp.toString()
                tempMax.text = "Max Temp: ${it.main.temp_max} C"
                tempMin.text = "Min Temp: ${it.main.temp_min} C"
                val humidity = "${it.main.humidity} %"
                val wind = "${it.wind.speed} m/s"
                val sunrise = time(it.sys.sunrise.toLong())
                val sunset = time(it.sys.sunrise.toLong())
                val sealevel = "${it.main.pressure} hPa"
                val condition = it.weather.firstOrNull()?.main ?: "unknown "
                weatherType.text = it.weather.firstOrNull()?.main ?: "unknown "
                day.text = dayName(System.currentTimeMillis())
                date.text = dateName(System.currentTimeMillis())

                // Observe changes to the LiveData
                dataListLiveData.observe(this, Observer { dataList ->
                    // Update the adapter with the new data list
                    (gridView.adapter as? GridAdapter)?.updateData(dataList)
                })

                val updatedDataList = mutableListOf<GridItems>(
                    GridItems(
                        R.drawable.humidity,
                        humidity,
                        "Humidity"
                    ),
                    GridItems(
                        R.drawable.wind,
                        wind,
                        "Wind Speed"
                    ),
                    GridItems(
                        R.drawable.conditions,
                        condition,
                        "Conditions"
                    ),
                    GridItems(
                        R.drawable.sunrise,
                        sunrise,
                        "Sunrise"
                    ),
                    GridItems(
                        R.drawable.sunset,
                        sunset,
                        "Sunset"
                    ),
                    GridItems(
                        R.drawable.sea,
                        sealevel,
                        "Sea"
                    ),
                )
                dataListLiveData.postValue(updatedDataList)
                changeLottie(condition)
            }
        }
    }

    private fun init() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted, get the last known location
            getLastLocation()
        } else {
            // Request location permissions
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location services are disabled, prompt the user to enable them
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            // Location services are enabled, proceed with getting the location
            // Add your location retrieval logic here
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                // Code to be delayed
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            // Got last known location. In some rare situations this can be null.
                            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            if (addresses != null) {
                                if (addresses.isNotEmpty()) {
                                    val cityName = addresses[0].locality
                                    retrofit(cityName)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@MainActivity,
                            "Error getting location: ${e.message}",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
            }
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        // Got last known location. In some rare situations this can be null.
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (addresses != null) {
                            if (addresses.isNotEmpty()) {
                                val cityName = addresses[0].locality
                                retrofit(cityName)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error getting location: ${e.message}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the last known location
                getLastLocation()
            } else {
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeLottie(condition: String) {
        when (condition) {
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                root.setBackgroundResource(R.drawable.cloud_background)
                lottie.setAnimation(R.raw.cloud)
            }

            "Light Rain", "Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                root.setBackgroundResource(R.drawable.rain_background)
                lottie.setAnimation(R.raw.rain)
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                root.setBackgroundResource(R.drawable.snow_background)
                lottie.setAnimation(R.raw.snow)
            }

            else -> {
                root.setBackgroundResource(R.drawable.sunny_background)
                lottie.setAnimation(R.raw.sun)
            }
        }
        lottie.playAnimation()
    }

    private fun dayName(currentTimeMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun dateName(currentTimeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun retrofit(query: String) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(RetrofitServices::class.java)
            val call = service.getWeatherData(query, "b560aa5c7e71afaacf08b1c87aa3d3e1", "metric")
            call.enqueue(object : retrofit2.Callback<WeatherApp> {
                override fun onResponse(
                    call: Call<WeatherApp>,
                    response: Response<WeatherApp>
                ) {
                    if (response.message() == "Not Found") {
                        Toast.makeText(
                            this@MainActivity,
                            "Incorrect State Name",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        weatherLiveData.postValue(response.body())
                        progressBar.visibility = View.GONE
                        root.alpha = 1.0f
                    }
                }

                override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    root.alpha = 1.0f
                    Toast.makeText(
                        this@MainActivity,
                        "Error fetching data: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun gridView(gridView: GridView) {
        val dataList = mutableListOf(
            GridItems(
                R.drawable.humidity,
                "",
                "Humidity"
            ),
            GridItems(
                R.drawable.wind,
                "",
                "Wind Speed"
            ),
            GridItems(
                R.drawable.conditions,
                "",
                "Conditions"
            ),
            GridItems(
                R.drawable.sunrise,
                "",
                "Sunrise"
            ),
            GridItems(
                R.drawable.sunset,
                "",
                "Sunset"
            ),
            GridItems(
                R.drawable.sea,
                "",
                "Sea"
            ),
        )
        val adapter = GridAdapter(this, dataList)
        gridView.adapter = adapter

    }
}
