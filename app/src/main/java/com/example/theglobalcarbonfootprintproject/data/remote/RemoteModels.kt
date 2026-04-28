package com.example.theglobalcarbonfootprintproject.data.remote

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class RemoteCarbonLog : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var userId: String = ""
    var date: Long = 0
    var totalKg: Double = 0.0
    var points: Int = 0
}

class RemoteUserProfile : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var userId: String = ""
    var name: String = ""
    var state: String = ""
    var totalPoints: Int = 0
}
