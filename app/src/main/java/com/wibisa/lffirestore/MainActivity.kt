package com.wibisa.lffirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val personCollectionRef = Firebase.firestore.collection("persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUploadData.setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
        }

        //subscribeToRealtimeUpdates()

        btnRetrieveData.setOnClickListener {
            retrievePersons()
        }

        btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPerson = getNewPersonMap()
            updatePerson(oldPerson, newPerson)
        }

        btnDeletePerson.setOnClickListener {
            val person = getOldPerson()
            deletePerson(person)
        }
    }

    private fun getOldPerson() : Person {
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val age = etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun getNewPersonMap() : Map<String, Any> {
        val firstName = etNewFirstName.text.toString()
        val lastName = etNewLastName.text.toString()
        val age = etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if (age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        return  map
    }

    //delete person data sesuai dengan data person old
    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionRef.document(document.id).delete().await() //delete document field
//                    personCollectionRef.document(document.id).update(mapOf(
//                        "firstName" to FieldValue.delete()
//                    )) //delete sesuai field yang mau di delete
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No person matched the query", Toast.LENGTH_LONG).show()
            }
        }
    }

    //update person data sesuai dengan data person old
    private fun updatePerson(person: Person, newPerson: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get()
                .await()

        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionRef.document(document.id).set(
                            newPerson,
                            SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No person matched the query", Toast.LENGTH_LONG).show()
            }
        }
    }

    //realtime update data from firestore using snapshot listener
    private fun subscribeToRealtimeUpdates() {
        personCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreExeption ->
            firebaseFirestoreExeption?.let{
                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                tvPersons.text = sb.toString()
            }
        }
    }

    //retrieve person data
    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch {
        //add query untuk panggil data
        val fromAge = etFrom.text.toString().toInt()
        val toAge = etTo.text.toString().toInt()

        try {
            //val querySnapshot = personCollectionRef.get().await() //mengambil semua data dari collection person from firestore
            val querySnapshot = personCollectionRef
                    .whereGreaterThan("age", fromAge)
                    .whereLessThan("age", toAge)
                    .orderBy("age")
                    .get()
                    .await() //mengambil data berdasarkan set custom query untuk coll person firestore

            val sb = StringBuilder()
            for (document in querySnapshot.documents) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main) {
                tvPersons.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //upload person data
    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}