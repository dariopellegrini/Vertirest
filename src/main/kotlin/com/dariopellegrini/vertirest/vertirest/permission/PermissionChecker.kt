package com.dariopellegrini.vertirest.vertirest.permission

import com.dariopellegrini.vertirest.vertirest.constants.RESTPermission
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

class PermissionChecker(val collectionName: String, private val mongo: MongoClient, val permissionsDescriptor: PermissionsDescriptor?) {
    private val COLLECTION = "user"

    fun checkCreateOne(token: String?, completion: (Boolean, JsonObject?) -> Unit) {
        if (permissionsDescriptor != null) {
            if (token != null) {
                findUser(token) {
                    userID ->
                    if (userID != null) {
                        completion(permissionsDescriptor.userPermission[RESTPermission.CREATE] ?: false, JsonObject().put("owner", userID))
                    } else {
                        completion(false, null)
                    }
                }
            } else {
                completion(permissionsDescriptor.guestPermission[RESTPermission.CREATE] ?: false, null)
            }
        } else {
            completion(true, null)
        }
    }

    fun checkAll(token: String?, permission: RESTPermission, completion: (Boolean, JsonObject?) -> Unit) {
        if (permissionsDescriptor != null) {
            if (token != null) {
                findUser(token) {
                    userID ->
                    if (userID != null) {
                        if (permissionsDescriptor.userPermission[permission] == true) { // Read all
                            completion(true, null)
                        } else if (permissionsDescriptor.ownerPermission[permission] == true) { // Read owned
                            completion(true, JsonObject().put("owner", userID))
                        } else {
                            completion(false, null)
                        }
                    } else {
                        completion(false, null)
                    }
                }
            } else {
                if (permissionsDescriptor.guestPermission[permission] == true) { // Read all
                    completion(true, null)
                }else {
                    completion(false, null)
                }
            }
        } else {
            completion(true, null)
        }
    }

    fun checkOne(token: String?,  _id: String?, permission: RESTPermission, completion: (Boolean, JsonObject?) -> Unit) {
        if (permissionsDescriptor != null) {
            if (token != null) {
                findUser(token) {
                    userID ->
                    if (userID != null) {
                        if (permissionsDescriptor.userPermission[permission] == true) { // Read the document
                            completion(true, null)
                        } else if (permissionsDescriptor.ownerPermission[permission] == true) { // Read only if owned
                            if (_id != null) {
                                searchOwner(_id, userID) {
                                    found ->
                                    if (found) {
                                        completion(true, JsonObject().put("owner", userID))
                                    } else {
                                        completion(false, null)
                                    }
                                }
                            } else {
                                completion(false, null)
                            }
                        } else {
                            completion(false, null)
                        }
                    } else {
                        completion(false, null)
                    }
                }
            } else {
                if (permissionsDescriptor.guestPermission[permission] == true) { // Read the document
                    completion(true, null)
                } else {
                    completion(false, null)
                }
            }
        } else {
            completion(true, null)
        }
    }

    // Owner
    fun searchOwner(_id: String, userID: String, completion: (Boolean) -> Unit) {
        mongo.findOne(collectionName, JsonObject().put("_id", _id).put("owner", userID), JsonObject("{\"owner\":1}"), { res ->
            if (res.succeeded()) {
                val resJson = res.result()
                if (resJson?.getString("owner") != null) {
                    completion(true)
                } else {
                    completion(false)
                }
            } else {
                completion(false)
            }
        })
    }

    // User
    private fun findUser(token: String, completion: (String?) -> Unit) {
        mongo.findOne(COLLECTION, JsonObject().put("token", token), JsonObject("{\"_id\":1}"), { res ->
            if (res.succeeded()) {
                val resJson = res.result()
                if (resJson?.getString("_id") != null) {
                    completion(resJson.getString("_id"))
                } else {
                    completion(null)
                }
            } else {
                completion(null)
            }
        })
    }
}