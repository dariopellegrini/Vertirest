package com.dariopellegrini.vertirest.vertirest.permission

import com.dariopellegrini.vertirest.vertirest.constants.RESTPermission

//fun VertirestObject.ownerPermission(permission: RESTPermission): Boolean {
//    return ownerPermission[permission] ?: false
//}
//
//fun VertirestObject.userPermission(permission: RESTPermission): Boolean {
//    return userPermission[permission] ?: false
//}
//
//fun VertirestObject.guestPermission(permission: RESTPermission): Boolean {
//    return guestPermission[permission] ?: false
//}

interface PermissionsDescriptor {

    val ownerPermission: Map<RESTPermission, Boolean>
    get() = mapOf(RESTPermission.READ to true,
            RESTPermission.READ_PUBLIC to true,
            RESTPermission.CREATE to true,
            RESTPermission.UPDATE  to true,
            RESTPermission.UPDATE to true)

    val userPermission: Map<RESTPermission, Boolean>
        get() = mapOf(RESTPermission.READ to true,
                RESTPermission.READ_PUBLIC to true,
                RESTPermission.CREATE to true,
                RESTPermission.UPDATE  to true,
                RESTPermission.UPDATE to true)

    val guestPermission: Map<RESTPermission, Boolean>
        get() = mapOf(RESTPermission.READ to true,
                RESTPermission.READ_PUBLIC to true,
                RESTPermission.CREATE to true,
                RESTPermission.UPDATE  to true,
                RESTPermission.UPDATE to true)

    val permissions: PermissionsContainer
        get() = PermissionsContainer(ownerPermission, userPermission, guestPermission)
}

