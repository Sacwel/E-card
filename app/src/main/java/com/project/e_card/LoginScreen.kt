package com.project.e_card

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.e_card.NFC.DataStoreUtils
import com.project.e_card.NFC.NFCDialogue
import com.project.e_card.Retrofit.ApiInterface
import com.project.e_card.Retrofit.EmployeeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.*
import retrofit2.converter.moshi.MoshiConverterFactory

private var TAG = "LoginActivity"
const val BASE_URL = "http://34.107.71.133:8000"

class LoginScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val firstNameField = findViewById<EditText>(R.id.employeeName)
        val employeeNumberField = findViewById<EditText>(R.id.employeeNumberInput)
        val employeePasswordField = findViewById<EditText>(R.id.employeePassword)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled) {
                NFCDialogue(this).showNFCDisabled()
            }
        } else{
            NFCDialogue(this).showNFCUnsupported()
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            when {
                firstNameField.text.toString().trim().isEmpty() -> {
                    firstNameField.error = "You must provide a name!"
                    Toast.makeText(this, "Name is required!", Toast.LENGTH_SHORT).show()
                }
                employeeNumberField.text.toString().trim().isEmpty() -> {
                    employeeNumberField.error = "You must provide employee number!"
                    Toast.makeText(this, "Employee number is required!", Toast.LENGTH_SHORT).show()
                }
                employeePasswordField.text.toString().trim().isEmpty() -> {
                    employeePasswordField.error = "You must provide a password!"
                    Toast.makeText(this, "Password is required!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    retLogin(intent, firstNameField.text.toString(), employeeNumberField.text.toString().toInt(), employeePasswordField.text.toString(), firstNameField, employeeNumberField, employeePasswordField)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val dataStore = DataStoreUtils(this)
        val uid = dataStore.getID()
        println("Current employees UID: $uid")

    }

    private fun retLogin(intent: Intent, firstNameField: String, employeeNumberField: Int, employeePasswordField: String, firstNameEditText: EditText, employeeNumberEditText: EditText, employeePasswordEditText: EditText) {
        val api = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(ApiInterface::class.java)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = api.getUserData(employeeNumberField).awaitResponse()
                Log.d(TAG, response.code().toString())
                if (response.isSuccessful) {

                    val firstName = response.body()!!.name
                    val lastName = response.body()!!.last_name
                    val employeeNumber = response.body()!!.employee_number
                    val password = response.body()!!.password
                    val email = response.body()!!.email
                    val uid = response.body()!!.UID
                    val accessLevel = response.body()!!.access_level

                    if (firstName == firstNameField && employeeNumber == employeeNumberField && password == employeePasswordField) {

                        //These three lines create an instance of the EmployeeData class and calls its makeObject method
                        val currentEmployeeData = EmployeeData()
                        currentEmployeeData.makeObject(firstName, lastName, employeeNumber, password, email, uid, accessLevel)

                        //Just testing that the data actually gets there, uncomment if testing is needed
                        //EmployeeData.printData()

                        startActivity(intent)
                    } else {
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            kotlin.run {
                                when {
                                    firstName != firstNameField -> firstNameEditText.error = "Incorrect first name!"
                                    employeeNumber != employeeNumberField -> employeeNumberEditText.error = "Incorrect employee number!"
                                    password != employeePasswordField -> employeePasswordEditText.error = "Incorrect password!"
                                }
                            }
                        })
                    }

                    /*for (i in 0..response.body()!!.size) {

                    // Loop for finding the correct user on the list after getting it to the device

                    }*/
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, e.toString())
                }
            }
        }
    }
}

